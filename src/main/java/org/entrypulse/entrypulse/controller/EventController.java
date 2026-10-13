
package org.entrypulse.entrypulse.controller;

import org.entrypulse.entrypulse.model.Event;
import org.entrypulse.entrypulse.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // Create a new event
    @PostMapping
    public ResponseEntity<Event> createEvent(
            @RequestBody Event event) {

        Event createdEvent = eventService.createEvent(event);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdEvent);
    }

    // Retrieve all events
    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {

        List<Event> events = eventService.getAllEvents();

        return ResponseEntity.ok(events);
    }

    // Retrieve a single event by ID
    @GetMapping("/{eventId}")
    public ResponseEntity<Event> getEventById(
            @PathVariable String eventId) {

        Event event = eventService.getEventById(eventId);

        return ResponseEntity.ok(event);
    }
}

