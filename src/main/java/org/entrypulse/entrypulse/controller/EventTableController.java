package org.entrypulse.entrypulse.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.dto.eventtable.CreateEventTableRequest;
import org.entrypulse.entrypulse.dto.eventtable.EventTableResponse;
import org.entrypulse.entrypulse.service.EventTableService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/event-tables")
@RequiredArgsConstructor
public class EventTableController {

    private final EventTableService eventTableService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventTableResponse createEventTable(
            @Valid @RequestBody CreateEventTableRequest request,
            Authentication authentication) {
        return EventTableResponse.from(
                eventTableService.createEventTable(request, authentication.getName()));
    }

    @GetMapping("/event/{eventId}")
    public List<EventTableResponse> getTablesForEvent(
            @PathVariable String eventId, Authentication authentication) {
        return eventTableService.getTablesForEvent(eventId, authentication.getName())
                .stream().map(EventTableResponse::from).toList();
    }

    @DeleteMapping("/{eventTableId}")
    public ResponseEntity<Void> deleteEventTable(
            @PathVariable String eventTableId, Authentication authentication) {
        eventTableService.deleteEventTable(eventTableId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}