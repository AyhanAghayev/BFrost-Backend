package com.bfrost.backend.event;

import com.bfrost.backend.club.Club;
import com.bfrost.backend.club.ClubRepository;
import com.bfrost.backend.club.MemberRole;
import com.bfrost.backend.club.MembershipRepository;
import com.bfrost.backend.common.exception.ConflictException;
import com.bfrost.backend.common.exception.ForbiddenException;
import com.bfrost.backend.common.exception.ResourceNotFoundException;
import com.bfrost.backend.event.dto.AttendeeDto;
import com.bfrost.backend.event.dto.CreateEventRequest;
import com.bfrost.backend.event.dto.EventDto;
import com.bfrost.backend.event.dto.UpdateEventRequest;
import com.bfrost.backend.user.User;
import com.bfrost.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final RsvpRepository rsvpRepository;
    private final ClubRepository clubRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<EventDto> getClubEvents(UUID clubId, UUID currentUserId) {
        return eventRepository.findByClubIdAndStartTimeAfterOrderByStartTimeAsc(clubId, Instant.now())
                .stream()
                .map(e -> buildDto(e, currentUserId))
                .toList();
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
        ClubEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        return buildDto(event, currentUserId);
    }

    @Transactional
    public EventDto create(UUID clubId, CreateEventRequest req, UUID creatorId) {
        requireModeratorOrOwner(clubId, creatorId);
        Club club = clubRepository.getReferenceById(clubId);
        User creator = userRepository.getReferenceById(creatorId);
        ClubEvent event = ClubEvent.builder()
                .club(club)
                .title(req.title())
                .description(req.description())
                .format(req.format())
                .location(req.location())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .maxMembers(req.maxMembers())
                .coverImageUrl(req.coverImageUrl())
                .createdBy(creator)
                .build();
        eventRepository.save(event);
        return buildDto(event, creatorId);
    }

    @Transactional
    public EventDto update(UUID eventId, UpdateEventRequest req, UUID userId) {
        ClubEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
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
        return buildDto(event, userId);
    }

    @Transactional
    public void delete(UUID eventId, UUID userId) {
        ClubEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        requireModeratorOrOwner(event.getClub().getId(), userId);
        eventRepository.delete(event);
    }

    @Transactional(readOnly = true)
    public List<AttendeeDto> getAttendees(UUID eventId, UUID userId) {
        ClubEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        requireModeratorOrOwner(event.getClub().getId(), userId);
        return rsvpRepository.findByEventIdAndStatusOrderByRespondedAtAsc(eventId, RsvpStatus.ATTENDING)
                .stream()
                .map(AttendeeDto::from)
                .toList();
    }

    @Transactional
    public void rsvp(UUID eventId, UUID userId, RsvpStatus status) {
        ClubEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        if (status == RsvpStatus.ATTENDING && event.getMaxMembers() != null) {
            long attending = rsvpRepository.countByEventIdAndStatus(eventId, RsvpStatus.ATTENDING);
            if (attending >= event.getMaxMembers()) throw new ConflictException("Event is full");
        }
        Rsvp rsvp = rsvpRepository.findByEventIdAndUserId(eventId, userId)
                .orElseGet(() -> Rsvp.builder().event(event).user(userRepository.getReferenceById(userId)).build());
        rsvp.setStatus(status);
        rsvp.setRespondedAt(Instant.now());
        rsvpRepository.save(rsvp);
    }

    @Transactional(readOnly = true)
    public List<EventDto> getMyFeed(UUID userId) {
        return eventRepository.findUpcomingEventsForUser(userId).stream()
                .map(e -> buildDto(e, userId))
                .toList();
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
                .orElseThrow(() -> new ForbiddenException("Moderator or owner required to create events"));
    }

    private EventDto buildDto(ClubEvent event, UUID currentUserId) {
        long attending = rsvpRepository.countByEventIdAndStatus(event.getId(), RsvpStatus.ATTENDING);
        String rsvp = currentUserId == null ? null :
                rsvpRepository.findByEventIdAndUserId(event.getId(), currentUserId)
                        .map(r -> r.getStatus().name()).orElse(null);
        return EventDto.from(event, attending, rsvp);
    }
}