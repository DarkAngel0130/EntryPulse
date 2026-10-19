
package org.entrypulse.entrypulse.service;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.model.Event;
import org.entrypulse.entrypulse.model.User;
import org.entrypulse.entrypulse.repository.EventRepository;
import org.entrypulse.entrypulse.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    private User getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required"
            );
        }

        String userId = authentication.getName();

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));
    }

    private User getAuthenticatedOrganiser() {
        User user = getAuthenticatedUser();

        if (user.getRole() != User.Role.ORGANISER) {
            throw new AccessDeniedException(
                    "Only organisers can perform this operation"
            );
        }

        return user;
    }

    public Event createEvent(Event event) {
        User organiser = getAuthenticatedOrganiser();

        // Never trust an organiser ID supplied by the client.
        event.setEventId(null);
        event.setOrganiser(organiser);

        return eventRepository.save(event);
    }

    public List<Event> getAllEvents() {
        User organiser = getAuthenticatedOrganiser();

        return eventRepository.findByOrganiser_Id(organiser.getId());
    }

    public Event getEventById(String eventId) {
        User organiser = getAuthenticatedOrganiser();

        return eventRepository
                .findByEventIdAndOrganiser_Id(
                        eventId,
                        organiser.getId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"
                ));
    }


    public Event updateEvent(String eventId, Event updatedEvent) {
        User organiser = getAuthenticatedOrganiser();

        Event existingEvent = eventRepository
                .findByEventIdAndOrganiser_Id(
                        eventId,
                        organiser.getId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"
                ));

        // Update only the fields the organiser is allowed to change.
        existingEvent.setName(updatedEvent.getName());
        existingEvent.setDate(updatedEvent.getDate());
        existingEvent.setTime(updatedEvent.getTime());
        existingEvent.setLocation(updatedEvent.getLocation());

        return eventRepository.save(existingEvent);
    }

    public void deleteEvent(String eventId) {
        User organiser = getAuthenticatedOrganiser();

        Event event = eventRepository
                .findByEventIdAndOrganiser_Id(
                        eventId,
                        organiser.getId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"
                ));

        eventRepository.delete(event);
    }

}

