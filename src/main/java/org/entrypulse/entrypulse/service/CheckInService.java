package org.entrypulse.entrypulse.service;


import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.model.CheckIn;
import org.entrypulse.entrypulse.model.EventGuest;
import org.entrypulse.entrypulse.model.Invitation;
import org.entrypulse.entrypulse.repository.CheckInRepository;
import org.entrypulse.entrypulse.repository.InvitationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CheckInService {
    private final InvitationRepository invitationRepository;
    private final CheckInRepository checkInRepository;


    public CheckIn checkInGuest(String token) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow( ()->
                    new RuntimeException("Invalid QR Code")
                );

        EventGuest eventGuest = invitation.getEventGuest();

        var guest = eventGuest.getGuest();
        var event = eventGuest.getEvent();

        boolean alreadyCheckedIn =
                checkInRepository.existsByGuestIdAndEventEventId(guest.getId(), event.getEventId());

        if (alreadyCheckedIn) {
            throw new RuntimeException("Guest already checked in");
        }

        CheckIn checkIn = new CheckIn();

        checkIn.setGuest(guest);
        checkIn.setEvent(event);
        checkIn.setCheckInTime(LocalDateTime.now());


        return checkInRepository.save(checkIn);
    }
}
