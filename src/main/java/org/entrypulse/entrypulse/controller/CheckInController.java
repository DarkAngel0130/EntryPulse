package org.entrypulse.entrypulse.controller;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.dto.checkin.CheckInResponse;
import org.entrypulse.entrypulse.model.CheckIn;
import org.entrypulse.entrypulse.service.CheckInService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.entrypulse.entrypulse.dto.checkin.InvitationValidationResponse;
import org.entrypulse.entrypulse.dto.checkin.GuestSearchResponse;

import java.util.List;

@RestController
@RequestMapping("/api/check-ins")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    public record CheckInRequest(String token) {}

    // Check in a guest using their QR invitation token
    @PostMapping
    public ResponseEntity<CheckInResponse> checkInGuest(
            @RequestBody CheckInRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                CheckInResponse.from(
                        checkInService.checkInGuest(
                                request.token(),
                                authentication.getName()
                        )
                )
        );
    }

    // Get all check-ins for an event (organiser access)
    @GetMapping("/event/{eventId}")
    public List<CheckInResponse> forEvent(
            @PathVariable String eventId,
            Authentication authentication
    ) {
        return checkInService
                .getCheckInsForEvent(eventId, authentication.getName())
                .stream()
                .map(CheckInResponse::from)
                .toList();
    }

    // Get check-ins performed by the authenticated security officer
    @GetMapping("/event/{eventId}/my-activity")
    public List<CheckInResponse> myActivity(
            @PathVariable String eventId,
            Authentication authentication
    ) {
        return checkInService
                .getMyCheckInsForEvent(
                        eventId,
                        authentication.getName()
                )
                .stream()
                .map(CheckInResponse::from)
                .toList();
    }

    // Get the authenticated security officer's check-in count for an event
    @GetMapping("/event/{eventId}/my-count")
    public ResponseEntity<Long> myCount(
            @PathVariable String eventId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                checkInService.getMyCheckInCountForEvent(
                        eventId,
                        authentication.getName()
                )
        );
    }

    public record ValidateInvitationRequest(
            String token,
            String eventId
    ) {}

    @PostMapping("/validate")
    public ResponseEntity<InvitationValidationResponse> validateInvitation(
            @RequestBody ValidateInvitationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                checkInService.validateInvitation(
                        request.token(),
                        request.eventId(),
                        authentication.getName()
                )
        );
    }


    @GetMapping("/event/{eventId}/guests/search")
    public ResponseEntity<List<GuestSearchResponse>> searchGuests(
            @PathVariable String eventId,
            @RequestParam String name,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                checkInService.searchGuestsForEvent(
                        eventId,
                        name,
                        authentication.getName()
                )
        );
    }

}