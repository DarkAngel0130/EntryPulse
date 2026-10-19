
package org.entrypulse.entrypulse.service;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.model.CheckIn;
import org.entrypulse.entrypulse.model.EventGuest;
import org.entrypulse.entrypulse.model.EventSecurity;
import org.entrypulse.entrypulse.model.Invitation;
import org.entrypulse.entrypulse.model.User;
import org.entrypulse.entrypulse.repository.CheckInRepository;
import org.entrypulse.entrypulse.repository.EventSecurityRepository;
import org.entrypulse.entrypulse.repository.InvitationRepository;
import org.entrypulse.entrypulse.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CheckInService {

    private final InvitationRepository invitationRepository;
    private final CheckInRepository checkInRepository;
    private final EventSecurityRepository eventSecurityRepository;
    private final UserRepository userRepository;

    @Transactional
    public CheckIn checkInGuest(String token, String securityUserId) {

        User securityUser = userRepository.findById(securityUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        if (securityUser.getRole() != User.Role.SECURITY) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only security officers can check in guests");
        }

        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invalid QR code"));

        if (invitation.getStatus() != Invitation.InvitationStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invitation is not active");
        }

        EventGuest eventGuest = invitation.getEventGuest();

        var guest = eventGuest.getGuest();
        var event = eventGuest.getEvent();

        boolean assigned = eventSecurityRepository
                .existsByEvent_EventIdAndUser_IdAndStatus(
                        event.getEventId(),
                        securityUserId,
                        EventSecurity.AssignmentStatus.ACCEPTED);

        if (!assigned) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorised to check in guests for this event");
        }

        boolean alreadyCheckedIn =
                checkInRepository.existsByGuestIdAndEventEventId(
                        guest.getId(), event.getEventId());

        if (alreadyCheckedIn) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Guest has already checked in");
        }

        CheckIn checkIn = new CheckIn();
        checkIn.setGuest(guest);
        checkIn.setEvent(event);
        checkIn.setCheckedInBy(securityUser);
        checkIn.setCheckInTime(LocalDateTime.now());

        CheckIn savedCheckIn = checkInRepository.save(checkIn);

        invitation.setStatus(Invitation.InvitationStatus.USED);
        invitationRepository.save(invitation);

        return savedCheckIn;
    }
}
