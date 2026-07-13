package com.bfrost.backend.event;

import com.bfrost.backend.auth.BFrostUserDetails;
import com.bfrost.backend.event.dto.AttendeeDto;
import com.bfrost.backend.event.dto.CreateEventRequest;
import com.bfrost.backend.event.dto.EventDto;
import com.bfrost.backend.event.dto.RegistrationDto;
import com.bfrost.backend.event.dto.UpdateEventRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping("/events")
    public List<EventDto> getMyEvents(@AuthenticationPrincipal BFrostUserDetails principal) {
        return eventService.getMyFeed(principal.userId());
    }

    @GetMapping("/clubs/{clubSlug}/events")
    public List<EventDto> getClubEvents(@PathVariable String clubSlug,
                                        @RequestParam(defaultValue = "false") boolean includePast,
                                        @AuthenticationPrincipal BFrostUserDetails principal) {
        return eventService.getClubEventsBySlug(clubSlug,
                principal != null ? principal.userId() : null, includePast);
    }

    @PostMapping("/clubs/{clubId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventDto create(@PathVariable UUID clubId,
                           @Valid @RequestBody CreateEventRequest req,
                           @AuthenticationPrincipal BFrostUserDetails principal) {
        return eventService.create(clubId, req, principal.userId());
    }

    @GetMapping("/events/{eventId}")
    public EventDto getEvent(@PathVariable UUID eventId,
                             @AuthenticationPrincipal BFrostUserDetails principal) {
        return eventService.getEvent(eventId, principal != null ? principal.userId() : null);
    }

    @PatchMapping("/events/{eventId}")
    public EventDto update(@PathVariable UUID eventId,
                           @Valid @RequestBody UpdateEventRequest req,
                           @AuthenticationPrincipal BFrostUserDetails principal) {
        return eventService.update(eventId, req, principal.userId());
    }

    @DeleteMapping("/events/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID eventId,
                       @AuthenticationPrincipal BFrostUserDetails principal) {
        eventService.delete(eventId, principal.userId());
    }

    @GetMapping("/events/{eventId}/rsvps")
    public List<AttendeeDto> getAttendees(@PathVariable UUID eventId,
                                          @AuthenticationPrincipal BFrostUserDetails principal) {
        return eventService.getAttendees(eventId, principal.userId());
    }

    @PostMapping("/events/{eventId}/rsvp")
    public RegistrationDto.RsvpResultDto rsvp(@PathVariable UUID eventId,
                                              @Valid @RequestBody RegistrationDto.RsvpRequest req,
                                              @AuthenticationPrincipal BFrostUserDetails principal) {
        return eventService.rsvp(eventId, principal.userId(), req);
    }

    // Organizer view of registrants + their form answers.
    @GetMapping("/events/{eventId}/responses")
    public List<RegistrationDto.ResponseDto> responses(@PathVariable UUID eventId,
                                                       @AuthenticationPrincipal BFrostUserDetails principal) {
        return eventService.getResponses(eventId, principal.userId());
    }

    public record CheckInRequest(boolean attended) {}

    @PostMapping("/events/{eventId}/attendees/{userId}/checkin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void checkIn(@PathVariable UUID eventId,
                        @PathVariable("userId") UUID targetUserId,
                        @RequestBody CheckInRequest req,
                        @AuthenticationPrincipal BFrostUserDetails principal) {
        eventService.checkIn(eventId, targetUserId, principal.userId(), req.attended());
    }
}
