
package org.entrypulse.entrypulse.service;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.model.EventGuest;
import org.entrypulse.entrypulse.model.EventSecurity;
import org.entrypulse.entrypulse.model.Invitation;
import org.entrypulse.entrypulse.repository.EventGuestRepository;
import org.entrypulse.entrypulse.repository.EventRepository;
import org.entrypulse.entrypulse.repository.InvitationRepository;
import org.entrypulse.entrypulse.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final EventGuestRepository eventGuestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    public Invitation createInvitation(
            String eventGuestId,
            String organiserId) {

        var organiser = userRepository.findById(organiserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        if (organiser.getRole()
                != org.entrypulse.entrypulse.model.User.Role.ORGANISER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only organisers can create invitations");
        }

        EventGuest eventGuest = eventGuestRepository.findById(eventGuestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event guest not found"));

        if (!eventGuest.getEvent().getOrganiser().getId()
                .equals(organiserId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Event guest not found");
        }

        if (invitationRepository
                .existsByEventGuest_EventGuestId(eventGuestId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An invitation already exists for this event guest");
        }

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        Invitation invitation = new Invitation();
        invitation.setToken(token);
        invitation.setStatus(Invitation.InvitationStatus.ACTIVE);
        invitation.setEventGuest(eventGuest);
        invitation.setGuest(eventGuest.getGuest());
        invitation.setEvent(eventGuest.getEvent());

        return invitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public Invitation validateInvitation(String token) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invitation not found"));

        if (invitation.getStatus()
                != Invitation.InvitationStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invitation is not active");
        }

        return invitation;
    }

    public Invitation cancelInvitation(
            String invitationId,
            String organiserId) {

        Invitation invitation = invitationRepository
                .findByIdAndEvent_Organiser_Id(
                        invitationId, organiserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invitation not found"));

        if (invitation.getStatus()
                != Invitation.InvitationStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only active invitations can be cancelled");
        }

        invitation.setStatus(Invitation.InvitationStatus.CANCELLED);

        return invitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public String getInvitationTokenForOrganiser(
            String invitationId,
            String organiserId) {

        Invitation invitation = invitationRepository
                .findByIdAndEvent_Organiser_Id(
                        invitationId, organiserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invitation not found"));

        if (invitation.getStatus()
                != Invitation.InvitationStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only active invitations can generate QR codes");
        }

        return invitation.getToken();
    }


    @Transactional(readOnly = true)
    public List<Invitation> getInvitationsForEvent(
            String eventId,
            String organiserId) {

        var organiser = userRepository.findById(organiserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        if (organiser.getRole()
                != org.entrypulse.entrypulse.model.User.Role.ORGANISER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only organisers can view invitations");
        }

        eventRepository.findByEventIdAndOrganiser_Id(
                eventId, organiserId
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Event not found"));

        return invitationRepository
                .findByEvent_EventIdAndEvent_Organiser_Id(
                        eventId, organiserId);
    }

    public Invitation reinstateInvitation(
            String invitationId,
            String organiserId) {

        var organiser = userRepository.findById(organiserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        if (organiser.getRole()
                != org.entrypulse.entrypulse.model.User.Role.ORGANISER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only organisers can reinstate invitations");
        }

        Invitation invitation = invitationRepository
                .findByIdAndEvent_Organiser_Id(
                        invitationId, organiserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invitation not found"));

        if (invitation.getStatus()
                != Invitation.InvitationStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only cancelled invitations can be reinstated");
        }

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        invitation.setToken(token);
        invitation.setStatus(Invitation.InvitationStatus.ACTIVE);

        return invitationRepository.save(invitation);
    }



}
