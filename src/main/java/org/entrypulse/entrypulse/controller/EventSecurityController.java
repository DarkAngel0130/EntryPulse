package org.entrypulse.entrypulse.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.dto.security.EventSecurityResponse;
import org.entrypulse.entrypulse.service.EventSecurityService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/event-security")
@RequiredArgsConstructor
public class EventSecurityController {

    private final EventSecurityService service;

    public record AssignByEmailRequest(
            @NotBlank String eventId,
            @NotBlank @Email String email) {}

    @PostMapping("/assign")
    public EventSecurityResponse assign(
            @RequestParam String eventId, @RequestParam String userId,
            Authentication auth) {
        return EventSecurityResponse.from(
                service.assignSecurityToEvent(eventId, userId, auth.getName()));
    }

    @PostMapping("/assign-by-email")
    public EventSecurityResponse assignByEmail(
            @Valid @RequestBody AssignByEmailRequest request, Authentication auth) {
        return EventSecurityResponse.from(
                service.assignSecurityByEmail(request.eventId(), request.email(), auth.getName()));
    }

    @GetMapping("/event/{eventId}")
    public List<EventSecurityResponse> forEvent(
            @PathVariable String eventId, Authentication auth) {
        return service.getSecurityForEvent(eventId, auth.getName())
                .stream().map(EventSecurityResponse::from).toList();
    }

    @GetMapping("/my-assignments")
    public List<EventSecurityResponse> mine(Authentication auth) {
        return service.getMyAssignments(auth.getName())
                .stream().map(EventSecurityResponse::from).toList();
    }

    @PostMapping("/{assignmentId}/accept")
    public EventSecurityResponse accept(@PathVariable String assignmentId, Authentication auth) {
        return EventSecurityResponse.from(service.acceptAssignment(assignmentId, auth.getName()));
    }

    @PostMapping("/{assignmentId}/decline")
    public EventSecurityResponse decline(@PathVariable String assignmentId, Authentication auth) {
        return EventSecurityResponse.from(service.declineAssignment(assignmentId, auth.getName()));
    }

    @PostMapping("/{assignmentId}/revoke")
    public EventSecurityResponse revoke(@PathVariable String assignmentId, Authentication auth) {
        return EventSecurityResponse.from(service.revokeAssignment(assignmentId, auth.getName()));
    }

    @PostMapping("/{assignmentId}/reinstate")
    public EventSecurityResponse reinstate(@PathVariable String assignmentId, Authentication auth) {
        return EventSecurityResponse.from(service.reinstateAssignment(assignmentId, auth.getName()));
    }
}