package org.entrypulse.entrypulse.service;


import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.model.Event;
import org.entrypulse.entrypulse.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;

    public Event createEvent(Event event) {
        return eventRepository.save(event);
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Event getEventById(String id) {
        return eventRepository.findById(id).orElse(null);
    }
}
