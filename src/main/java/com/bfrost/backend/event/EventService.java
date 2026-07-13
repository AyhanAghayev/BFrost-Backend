package com.bfrost.backend.event;

import com.bfrost.backend.club.Club;
import com.bfrost.backend.club.ClubRepository;
import com.bfrost.backend.club.MemberRole;
import com.bfrost.backend.club.MembershipRepository;
import com.bfrost.backend.common.exception.ForbiddenException;
import com.bfrost.backend.common.exception.ResourceNotFoundException;
import com.bfrost.backend.event.dto.AttendeeDto;
import com.bfrost.backend.event.dto.CreateEventRequest;
import com.bfrost.backend.event.dto.EventDto;
import com.bfrost.backend.event.dto.RegistrationDto;
import com.bfrost.backend.event.dto.UpdateEventRequest;
import com.bfrost.backend.user.User;
import com.bfrost.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final RsvpRepository rsvpRepository;
    private final RsvpAnswerRepository answerRepository;
    private final EventQuestionRepository questionRepository;
    private final ClubRepository clubRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<EventDto> getClubEvents(UUID clubId, UUID currentUserId) {
        return eventRepository.findByClubIdAndStartTimeAfterOrderByStartTimeAsc(clubId, Instant.now())
                .stream().map(e -> buildDto(e, currentUserId)).toList();
    }

    @Transactional(readOnly = true)
    public List<EventDto> getClubEventsBySlug(String clubSlug, UUID currentUserId, boolean includePast) {
        Club club = resolveClub(clubSlug);
        List<ClubEvent> events = includePast
                ? eventRepository.findByClubIdOrderByStartTimeAsc(club.getId())
                : eventRepository.findByClubIdAndStartTimeAfterOrderByStartTimeAsc(club.getId(), Instant.now());
        return events.stream().map(e -> buildDto(e, currentUserId)).toList();
    }

    @Transactional(readOnly = true)
    public EventDto getEvent(UUID eventId, UUID currentUserId) {
        return buildDto(loadEvent(eventId), currentUserId);
    }

    @Transactional
    public EventDto create(UUID clubId, CreateEventRequest req, UUID creatorId) {
        requireModeratorOrOwner(clubId, creatorId);
        Club club = clubRepository.getReferenceById(clubId);
        User creator = userRepository.getReferenceById(creatorId);
        ClubEvent event = ClubEvent.builder()
                .club(club).title(req.title()).description(req.description()).format(req.format())
                .location(req.location()).startTime(req.startTime()).endTime(req.endTime())
                .maxMembers(req.maxMembers()).coverImageUrl(req.coverImageUrl()).createdBy(creator)
                .build();
        eventRepository.save(event);
        replaceQuestions(event, req.questions());
        return buildDto(event, creatorId);
    }

    @Transactional
    public EventDto update(UUID eventId, UpdateEventRequest req, UUID userId) {
        ClubEvent event = loadEvent(eventId);
        requireModeratorOrOwner(event.getClub().getId(), userId);
        if (req.title() != null)       event.setTitle(req.title());
        if (req.description() != null) event.setDescription(req.description());
        if (req.format() != null)      event.setFormat(req.format());
        if (req.location() != null)    event.setLocation(req.location());
        if (req.startTime() != null)   event.setStartTime(req.startTime());
        if (req.endTime() != null)     event.setEndTime(req.endTime());
        if (req.maxMembers() != null)  event.setMaxMembers(req.maxMembers());
        if (req.coverImageUrl() != null) event.setCoverImageUrl(req.coverImageUrl());
        eventRepository.save(event);
        if (req.questions() != null) replaceQuestions(event, req.questions());
        return buildDto(event, userId);
    }

    @Transactional
    public void delete(UUID eventId, UUID userId) {
        ClubEvent event = loadEvent(eventId);
        requireModeratorOrOwner(event.getClub().getId(), userId);
        eventRepository.delete(event);
    }

    @Transactional
    public RegistrationDto.RsvpResultDto rsvp(UUID eventId, UUID userId, RegistrationDto.RsvpRequest req) {
        ClubEvent event = loadEvent(eventId);
        RsvpStatus requested = req.status();

        Rsvp rsvp = rsvpRepository.findByEventIdAndUserId(eventId, userId)
                .orElseGet(() -> Rsvp.builder().event(event).user(userRepository.getReferenceById(userId)).build());
        boolean wasAttending = rsvp.getId() != null && rsvp.getStatus() == RsvpStatus.ATTENDING;

        RsvpStatus effective = requested;
        if (requested == RsvpStatus.ATTENDING && event.getMaxMembers() != null && !wasAttending) {
            long attending = rsvpRepository.countByEventIdAndStatus(eventId, RsvpStatus.ATTENDING);
            if (attending >= event.getMaxMembers()) effective = RsvpStatus.WAITLISTED;
        }

        if (effective != RsvpStatus.NOT_ATTENDING) {
            validateAnswers(eventId, req.answers());
        }

        rsvp.setStatus(effective);
        rsvp.setRespondedAt(Instant.now());
        if (effective == RsvpStatus.NOT_ATTENDING) rsvp.setAttended(false);
        rsvpRepository.save(rsvp);

        // Replace this registrant's answers.
        answerRepository.deleteByRsvpId(rsvp.getId());
        if (effective != RsvpStatus.NOT_ATTENDING && req.answers() != null) {
            saveAnswers(rsvp, req.answers());
        }

        // Freeing a spot? Promote the oldest waitlisted registrant.
        if (wasAttending && effective == RsvpStatus.NOT_ATTENDING) {
            promoteFromWaitlist(event);
        }
        return new RegistrationDto.RsvpResultDto(effective.name());
    }

    @Transactional(readOnly = true)
    public List<AttendeeDto> getAttendees(UUID eventId, UUID userId) {
        ClubEvent event = loadEvent(eventId);
        requireModeratorOrOwner(event.getClub().getId(), userId);
        return rsvpRepository.findByEventIdAndStatusOrderByRespondedAtAsc(eventId, RsvpStatus.ATTENDING)
                .stream().map(AttendeeDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RegistrationDto.ResponseDto> getResponses(UUID eventId, UUID userId) {
        ClubEvent event = loadEvent(eventId);
        requireModeratorOrOwner(event.getClub().getId(), userId);
        List<Rsvp> attending = rsvpRepository.findByEventIdAndStatusOrderByRespondedAtAsc(eventId, RsvpStatus.ATTENDING);
        List<Rsvp> waitlisted = rsvpRepository.findByEventIdAndStatusOrderByRespondedAtAsc(eventId, RsvpStatus.WAITLISTED);
        List<Rsvp> all = new ArrayList<>(attending);
        all.addAll(waitlisted);

        Map<UUID, List<RegistrationDto.AnswerDto>> byRsvp = answerRepository
                .findByRsvpIdIn(all.stream().map(Rsvp::getId).toList()).stream()
                .collect(Collectors.groupingBy(a -> a.getRsvp().getId(),
                        Collectors.mapping(a -> new RegistrationDto.AnswerDto(
                                a.getQuestion().getId(), a.getQuestion().getLabel(), a.getValue()), Collectors.toList())));

        return all.stream().map(r -> new RegistrationDto.ResponseDto(
                r.getUser().getId(), r.getUser().getUsername(), r.getUser().getDisplayName(),
                r.getUser().getProfilePictureUrl(), r.getStatus().name(), r.isAttended(),
                byRsvp.getOrDefault(r.getId(), List.of())
        )).toList();
    }

    @Transactional
    public void checkIn(UUID eventId, UUID targetUserId, UUID actorId, boolean attended) {
        ClubEvent event = loadEvent(eventId);
        requireModeratorOrOwner(event.getClub().getId(), actorId);
        Rsvp rsvp = rsvpRepository.findByEventIdAndUserId(eventId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("This user has not registered"));
        rsvp.setAttended(attended);
        rsvpRepository.save(rsvp);
    }

    @Transactional(readOnly = true)
    public List<EventDto> getMyFeed(UUID userId) {
        return eventRepository.findUpcomingEventsForUser(userId).stream()
                .map(e -> buildDto(e, userId)).toList();
    }

    private ClubEvent loadEvent(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }

    private void promoteFromWaitlist(ClubEvent event) {
        if (event.getMaxMembers() == null) return;
        long attending = rsvpRepository.countByEventIdAndStatus(event.getId(), RsvpStatus.ATTENDING);
        if (attending >= event.getMaxMembers()) return;
        rsvpRepository.findFirstByEventIdAndStatusOrderByRespondedAtAsc(event.getId(), RsvpStatus.WAITLISTED)
                .ifPresent(next -> { next.setStatus(RsvpStatus.ATTENDING); rsvpRepository.save(next); });
    }

    private void replaceQuestions(ClubEvent event, List<RegistrationDto.QuestionInput> inputs) {
        questionRepository.deleteByEventId(event.getId());
        questionRepository.flush();
        if (inputs == null || inputs.isEmpty()) return;
        for (int i = 0; i < inputs.size(); i++) {
            RegistrationDto.QuestionInput in = inputs.get(i);
            boolean choice = in.type() == QuestionType.SINGLE_CHOICE || in.type() == QuestionType.MULTI_CHOICE;
            questionRepository.save(EventQuestion.builder()
                    .event(event).label(in.label()).type(in.type()).required(in.required()).position(i)
                    .options(choice && in.options() != null ? new ArrayList<>(in.options()) : new ArrayList<>())
                    .build());
        }
    }

    private void validateAnswers(UUID eventId, List<RegistrationDto.AnswerInput> answers) {
        Map<UUID, String> provided = answers == null ? Map.of() :
                answers.stream().filter(a -> a.questionId() != null)
                        .collect(Collectors.toMap(RegistrationDto.AnswerInput::questionId,
                                a -> a.value() == null ? "" : a.value(), (a, b) -> b));
        for (EventQuestion q : questionRepository.findByEventIdOrderByPosition(eventId)) {
            if (q.isRequired() && !StringUtils.hasText(provided.get(q.getId()))) {
                throw new ForbiddenException("Please answer: " + q.getLabel());
            }
        }
    }

    private void saveAnswers(Rsvp rsvp, List<RegistrationDto.AnswerInput> answers) {
        Map<UUID, EventQuestion> questions = questionRepository
                .findByEventIdOrderByPosition(rsvp.getEvent().getId()).stream()
                .collect(Collectors.toMap(EventQuestion::getId, q -> q));
        for (RegistrationDto.AnswerInput a : answers) {
            EventQuestion q = questions.get(a.questionId());
            if (q == null || !StringUtils.hasText(a.value())) continue; // ignore stray/empty
            answerRepository.save(RsvpAnswer.builder().rsvp(rsvp).question(q).value(a.value()).build());
        }
    }

    private Club resolveClub(String identifier) {
        try {
            UUID id = UUID.fromString(identifier);
            return clubRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Club not found: " + identifier));
        } catch (IllegalArgumentException notAUuid) {
            return clubRepository.findBySlug(identifier)
                    .orElseThrow(() -> new ResourceNotFoundException("Club not found: " + identifier));
        }
    }

    private void requireModeratorOrOwner(UUID clubId, UUID userId) {
        membershipRepository.findByClubIdAndUserId(clubId, userId)
                .filter(m -> m.getRole() != MemberRole.MEMBER)
                .orElseThrow(() -> new ForbiddenException("Moderator or owner required"));
    }

    private EventDto buildDto(ClubEvent event, UUID currentUserId) {
        long attending = rsvpRepository.countByEventIdAndStatus(event.getId(), RsvpStatus.ATTENDING);
        long waitlist = rsvpRepository.countByEventIdAndStatus(event.getId(), RsvpStatus.WAITLISTED);
        String rsvp = currentUserId == null ? null :
                rsvpRepository.findByEventIdAndUserId(event.getId(), currentUserId)
                        .map(r -> r.getStatus().name()).orElse(null);
        List<EventQuestion> questions = questionRepository.findByEventIdOrderByPosition(event.getId());
        return EventDto.from(event, attending, waitlist, rsvp, questions);
    }
}
