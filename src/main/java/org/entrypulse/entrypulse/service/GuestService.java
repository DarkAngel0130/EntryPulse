
package org.entrypulse.entrypulse.service;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.model.Guest;
import org.entrypulse.entrypulse.repository.GuestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GuestService {

    private final GuestRepository guestRepository;

    public Guest createGuest(Guest guest) {
        return guestRepository.save(guest);
    }

    public List<Guest> getAllGuests() {
        return guestRepository.findAll();
    }

    public Guest getGuestById(String id) {
        return guestRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Guest not found"
                        ));
    }

    public Guest updateGuest(String id, Guest guestDetails) {
        Guest existingGuest = getGuestById(id);

        existingGuest.setName(guestDetails.getName());

        return guestRepository.save(existingGuest);
    }

    public void deleteGuest(String id) {
        Guest existingGuest = getGuestById(id);

        guestRepository.delete(existingGuest);
    }
}

