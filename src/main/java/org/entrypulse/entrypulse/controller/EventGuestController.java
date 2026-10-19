
package org.entrypulse.entrypulse.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.dto.guest.CreateEventGuestRequest;
import org.entrypulse.entrypulse.dto.guest.EventGuestResponse;
import org.entrypulse.entrypulse.dto.guest.UpdateEventGuestRequest;
import org.entrypulse.entrypulse.service.EventGuestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/event-guests")
@RequiredArgsConstructor
public class EventGuestController {

    private final EventGuestService eventGuestService;

    @PostMapping
    public ResponseEntity<EventGuestResponse> createEventGuest(
            @Valid @RequestBody CreateEventGuestRequest request) {

        EventGuestResponse response =
                eventGuestService.createEventGuest(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<EventGuestResponse>> getGuestsForEvent(
            @PathVariable String eventId) {

        return ResponseEntity.ok(
                eventGuestService.getGuestsForEvent(eventId));
    }



    @GetMapping("/{id}")
    public ResponseEntity<EventGuestResponse> getEventGuestById(
            @PathVariable String id) {

        return ResponseEntity.ok(
                eventGuestService.getEventGuestById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventGuestResponse> updateEventGuest(
            @PathVariable String id,
            @Valid @RequestBody UpdateEventGuestRequest request) {

        EventGuestResponse response =
                eventGuestService.updateEventGuest(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEventGuest(
            @PathVariable String id) {

        eventGuestService.deleteEventGuest(id);
        return ResponseEntity.noContent().build();
    }



}
