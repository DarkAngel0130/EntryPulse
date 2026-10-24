
package org.entrypulse.entrypulse.service;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.dto.checkin.InvitationValidationResponse;
import org.entrypulse.entrypulse.model.*;
import org.entrypulse.entrypulse.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.entrypulse.entrypulse.dto.checkin.GuestSearchResponse;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckInService {

    private final InvitationRepository invitationRepository;
    private final CheckInRepository checkInRepository;
    private final EventSecurityRepository eventSecurityRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventGuestRepository eventGuestRepository;

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
                checkInRepository.existsByGuest_IdAndEvent_EventId(
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

    @Transactional(readOnly = true)
    public List<CheckIn> getCheckInsForEvent(String eventId, String organiserId) {
        eventRepository.findByEventIdAndOrganiser_Id(eventId, organiserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found"));
        return checkInRepository.findByEvent_EventIdOrderByCheckInTimeDesc(eventId);
    }


    @Transactional(readOnly = true)
    public List<CheckIn> getMyCheckInsForEvent(
            String eventId,
            String securityUserId
    ) {
        User securityUser = userRepository.findById(securityUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));

        if (securityUser.getRole() != User.Role.SECURITY) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only security officers can access this dashboard"
            );
        }

        boolean assigned = eventSecurityRepository
                .existsByEvent_EventIdAndUser_IdAndStatus(
                        eventId,
                        securityUserId,
                        EventSecurity.AssignmentStatus.ACCEPTED
                );

        if (!assigned) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorised to access this event"
            );
        }

        return checkInRepository
                .findByEvent_EventIdAndCheckedInBy_IdOrderByCheckInTimeDesc(
                        eventId,
                        securityUserId
                );
    }

    @Transactional(readOnly = true)
    public long getMyCheckInCountForEvent(
            String eventId,
            String securityUserId
    ) {
        // Reuse the authorisation checks before returning the count.
        getMyCheckInsForEvent(eventId, securityUserId);

        return checkInRepository
                .countByEvent_EventIdAndCheckedInBy_Id(
                        eventId,
                        securityUserId
                );
    }

    private void validateSecurityAssignment(
            String eventId,
            String securityUserId
    ) {
        User securityUser = userRepository.findById(securityUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));

        if (securityUser.getRole() != User.Role.SECURITY) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only security officers can access this dashboard"
            );
        }

        boolean assigned = eventSecurityRepository
                .existsByEvent_EventIdAndUser_IdAndStatus(
                        eventId,
                        securityUserId,
                        EventSecurity.AssignmentStatus.ACCEPTED
                );

        if (!assigned) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorised to access this event"
            );
        }
    }


    @Transactional(readOnly = true)
    public InvitationValidationResponse validateInvitation(
            String token,
            String eventId,
            String securityUserId
    ) {
        User securityUser = userRepository.findById(securityUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));

        if (securityUser.getRole() != User.Role.SECURITY) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only security officers can scan invitations"
            );
        }

        boolean assigned = eventSecurityRepository
                .existsByEvent_EventIdAndUser_IdAndStatus(
                        eventId,
                        securityUserId,
                        EventSecurity.AssignmentStatus.ACCEPTED
                );

        if (!assigned) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorised to access this event"
            );
        }

        Invitation invitation = invitationRepository.findByToken(token)
                .orElse(null);

        // Reject unknown invitations and invitations for another event.
        if (invitation == null
                || invitation.getEventGuest() == null
                || invitation.getEventGuest().getEvent() == null
                || !eventId.equals(
                invitation.getEventGuest().getEvent().getEventId()
        )) {
            return InvitationValidationResponse.invalid();
        }

        EventGuest eventGuest = invitation.getEventGuest();
        Guest guest = eventGuest.getGuest();

        // Retrieve the seating assignment from the database.
        Integer tableNumber = eventGuest.getEventTable() != null
                ? eventGuest.getEventTable().getTableNumber()
                : null;

        String seatNumber = eventGuest.getSeatNumber();

        var existingCheckIn = checkInRepository
                .findByGuest_IdAndEvent_EventId(
                        guest.getId(),
                        eventId
                );

        if (existingCheckIn.isPresent()) {
            CheckIn checkIn = existingCheckIn.get();

            return new InvitationValidationResponse(
                    "ALREADY_CHECKED_IN",
                    guest.getName(),
                    eventGuest.getEvent().getName(),
                    checkIn.getCheckedInBy() != null
                            ? checkIn.getCheckedInBy().getName()
                            : null,
                    checkIn.getCheckInTime(),
                    tableNumber,
                    seatNumber
            );
        }

        if (invitation.getStatus() != Invitation.InvitationStatus.ACTIVE) {
            return InvitationValidationResponse.invalid();
        }

        return new InvitationValidationResponse(
                "VALID",
                guest.getName(),
                eventGuest.getEvent().getName(),
                null,
                null,
                tableNumber,
                seatNumber
        );
    }


    @Transactional(readOnly = true)
    public List<GuestSearchResponse> searchGuestsForEvent(
            String eventId,
            String name,
            String securityUserId
    ) {
        validateSecurityAssignment(eventId, securityUserId);

        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Guest name search cannot be empty"
            );
        }

        String searchTerm = name.trim();

        List<EventGuest> registrations =
                eventGuestRepository
                        .findByEvent_EventIdAndGuest_NameContainingIgnoreCase(
                                eventId,
                                searchTerm
                        );

        return registrations.stream()
                .map(registration -> {
                    Guest guest = registration.getGuest();

                    var existingCheckIn =
                            checkInRepository.findByGuest_IdAndEvent_EventId(
                                    guest.getId(),
                                    eventId
                            );

                    if (existingCheckIn.isPresent()) {
                        CheckIn checkIn = existingCheckIn.get();

                        return new GuestSearchResponse(
                                guest.getId(),
                                guest.getName(),
                                registration.getEventGuestId(),
                                registration.getSeatNumber(),
                                true,
                                checkIn.getCheckedInBy() != null
                                        ? checkIn.getCheckedInBy().getName()
                                        : null,
                                checkIn.getCheckInTime() != null
                                        ? checkIn.getCheckInTime().toString()
                                        : null
                        );
                    }

                    return new GuestSearchResponse(
                            guest.getId(),
                            guest.getName(),
                            registration.getEventGuestId(),
                            registration.getSeatNumber(),
                            false,
                            null,
                            null
                    );
                })
                .toList();
    }


}
