package org.entrypulse.entrypulse.service;


import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.model.EventGuest;
import org.entrypulse.entrypulse.repository.EventGuestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventGuestService {
    private final EventGuestRepository eventGuestRepository;


    public EventGuest createEventGuest(EventGuest eventGuest) {
        return eventGuestRepository.save(eventGuest);
    }


    public List<EventGuest> getGuestsForEvent(String eventId) {

        return eventGuestRepository.findByEventEventId(eventId);
    }
}
