package com.bfrost.backend.event;

import com.bfrost.backend.club.*;
import com.bfrost.backend.common.exception.ForbiddenException;
import com.bfrost.backend.common.exception.ResourceNotFoundException;
import com.bfrost.backend.event.dto.CreateEventRequest;
import com.bfrost.backend.event.dto.RegistrationDto;
import com.bfrost.backend.user.User;
import com.bfrost.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock private RsvpRepository rsvpRepository;
    @Mock private ClubRepository clubRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private UserRepository userRepository;
    @Mock private RsvpAnswerRepository rsvpAnswerRepository;
    @Mock private EventQuestionRepository eventQuestionRepository;
    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository, rsvpRepository, rsvpAnswerRepository, eventQuestionRepository, clubRepository, membershipRepository, userRepository);
    }

    @Test
    void createRejectsNonModerator() {
        UUID clubId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Membership member = Membership.builder().role(MemberRole.MEMBER).build();

        when(membershipRepository.findByClubIdAndUserId(clubId, creatorId))
                .thenReturn(Optional.of(member));

        CreateEventRequest req = new CreateEventRequest("Title", null, EventFormat.IN_PERSON, null,
                Instant.now(), null, null, null, List.of());

        assertThatThrownBy(() -> eventService.create(clubId, req, creatorId))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(clubRepository); // опционально — явно зафиксировать, что клуб вообще не запрашивался

    }

    @Test
    void rsvpWaitlistsWhenEventIsFull() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ClubEvent event = ClubEvent.builder().id(eventId).maxMembers(2).startTime(Instant.now()).build();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(rsvpRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
        when(rsvpRepository.countByEventIdAndStatus(eventId, RsvpStatus.ATTENDING)).thenReturn(2L);

        RegistrationDto.RsvpResultDto result = eventService.rsvp(eventId, userId,
                new RegistrationDto.RsvpRequest(RsvpStatus.ATTENDING, List.of()));

        assertThat(result.status()).isEqualTo(RsvpStatus.WAITLISTED.name());
    }

    @Test
    void rsvpAllowsDecliningEvenWhenFull() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ClubEvent event = ClubEvent.builder().id(eventId).maxMembers(1).startTime(Instant.now()).build();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(rsvpRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());

        eventService.rsvp(eventId, userId, new RegistrationDto.RsvpRequest(RsvpStatus.NOT_ATTENDING, List.of()));

        verify(rsvpRepository).save(any(Rsvp.class));
    }
}