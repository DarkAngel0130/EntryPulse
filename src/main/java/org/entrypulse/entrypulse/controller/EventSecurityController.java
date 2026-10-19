
package org.entrypulse.entrypulse.controller;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.model.EventSecurity;
import org.entrypulse.entrypulse.service.EventSecurityService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/event-security")
@RequiredArgsConstructor
public class EventSecurityController {

    private final EventSecurityService eventSecurityService;

    @PostMapping("/assign")
    public EventSecurity assignSecurityToEvent(
            @RequestParam String eventId,
            @RequestParam String userId,
            Authentication authentication) {

        return eventSecurityService.assignSecurityToEvent(
                eventId, userId, authentication.getName());
    }

    @GetMapping("/event/{eventId}")
    public List<EventSecurity> getSecurityForEvent(
            @PathVariable String eventId,
            Authentication authentication) {

        return eventSecurityService.getSecurityForEvent(
                eventId, authentication.getName());
    }

    @GetMapping("/my-assignments")
    public List<EventSecurity> getMyAssignments(
            Authentication authentication) {

        return eventSecurityService.getMyAssignments(
                authentication.getName());
    }

    @PostMapping("/{assignmentId}/accept")
    public EventSecurity acceptAssignment(
            @PathVariable String assignmentId,
            Authentication authentication) {

        return eventSecurityService.acceptAssignment(
                assignmentId, authentication.getName());
    }

    @PostMapping("/{assignmentId}/decline")
    public EventSecurity declineAssignment(
            @PathVariable String assignmentId,
            Authentication authentication) {

        return eventSecurityService.declineAssignment(
                assignmentId, authentication.getName());
    }

    @PostMapping("/{assignmentId}/revoke")
    public EventSecurity revokeAssignment(
            @PathVariable String assignmentId,
            Authentication authentication) {

        return eventSecurityService.revokeAssignment(
                assignmentId, authentication.getName());
    }


    @PostMapping("/{assignmentId}/reinstate")
    public EventSecurity reinstateAssignment(
            @PathVariable String assignmentId,
            Authentication authentication) {

        return eventSecurityService.reinstateAssignment(
                assignmentId, authentication.getName());
    }

}
