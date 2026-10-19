
package org.entrypulse.entrypulse.controller;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.model.Event;
import org.entrypulse.entrypulse.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<Event> createEvent(
            @RequestBody Event event) {

        Event createdEvent = eventService.createEvent(event);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdEvent);
    }

    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<Event> getEventById(
            @PathVariable String eventId) {

        return ResponseEntity.ok(
                eventService.getEventById(eventId)
        );
    }


    @PutMapping("/{eventId}")
    public ResponseEntity<Event> updateEvent(
            @PathVariable String eventId,
            @RequestBody Event event) {

        return ResponseEntity.ok(
                eventService.updateEvent(eventId, event)
        );
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable String eventId) {

        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }

}
