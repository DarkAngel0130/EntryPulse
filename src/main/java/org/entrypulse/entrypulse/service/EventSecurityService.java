package org.entrypulse.entrypulse.service;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.model.Event;
import org.entrypulse.entrypulse.model.EventSecurity;
import org.entrypulse.entrypulse.model.User;
import org.entrypulse.entrypulse.repository.EventRepository;
import org.entrypulse.entrypulse.repository.EventSecurityRepository;
import org.entrypulse.entrypulse.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventSecurityService {
    private final EventSecurityRepository eventSecurityRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventSecurity assignSecurityToEvent(String eventId, String userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != User.Role.SECURITY) {
            throw new RuntimeException("User must have SECURITY role");
        }

        if (eventSecurityRepository.existsByEventEventIdAndUserId(eventId, userId)) {
            throw new RuntimeException("Security user is already assigned to this event");
        }

        EventSecurity eventSecurity = new EventSecurity();
        eventSecurity.setEvent(event);
        eventSecurity.setUser(user);

        return eventSecurityRepository.save(eventSecurity);
    }

    public List<EventSecurity> getSecurityForEvent(String eventId) {
        return eventSecurityRepository.findByEventEventId(eventId);
    }

    public List<EventSecurity> getEventsForSecurityUser(String userId) {
        return eventSecurityRepository.findByUserId(userId);
    }
}
