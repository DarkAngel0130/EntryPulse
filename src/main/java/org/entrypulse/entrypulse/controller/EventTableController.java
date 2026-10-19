
package org.entrypulse.entrypulse.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.dto.eventtable.CreateEventTableRequest;
import org.entrypulse.entrypulse.model.EventTable;
import org.entrypulse.entrypulse.service.EventTableService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/event-tables")
@RequiredArgsConstructor
public class EventTableController {

    private final EventTableService eventTableService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventTable createEventTable(
            @Valid @RequestBody CreateEventTableRequest request) {

        return eventTableService.createEventTable(request);
    }

    @GetMapping("/event/{eventId}")
    public List<EventTable> getTablesForEvent(
            @PathVariable String eventId) {

        return eventTableService.getTablesForEvent(eventId);
    }

    @DeleteMapping("/{eventTableId}")
    public ResponseEntity<Void> deleteEventTable(
            @PathVariable String eventTableId) {

        eventTableService.deleteEventTable(eventTableId);
        return ResponseEntity.noContent().build();
    }
}
