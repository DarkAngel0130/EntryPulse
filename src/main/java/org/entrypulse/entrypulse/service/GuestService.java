package org.entrypulse.entrypulse.service;


import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.model.Event;
import org.entrypulse.entrypulse.model.Guest;
import org.entrypulse.entrypulse.repository.GuestRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GuestService {
    private final GuestRepository guestRepository;

    public Guest createGuest(Guest guest) {
        return guestRepository.save(guest);
    }

    public List<Guest> getAllGuest() {
        return guestRepository.findAll();
    }

    public Optional<Guest> getGuestById(String id) {
        return guestRepository.findById(id);
    }
}
