
package org.entrypulse.entrypulse.controller;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.model.Invitation;
import org.entrypulse.entrypulse.service.InvitationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Invitation createInvitation(
            @RequestParam String eventGuestId,
            Authentication authentication) {

        return invitationService.createInvitation(
                eventGuestId,
                authentication.getName());
    }

    @GetMapping("/validate")
    public Invitation validateInvitation(
            @RequestParam String token) {

        return invitationService.validateInvitation(token);
    }

    @PostMapping("/{invitationId}/cancel")
    public Invitation cancelInvitation(
            @PathVariable String invitationId,
            Authentication authentication) {

        return invitationService.cancelInvitation(
                invitationId,
                authentication.getName());
    }
}
