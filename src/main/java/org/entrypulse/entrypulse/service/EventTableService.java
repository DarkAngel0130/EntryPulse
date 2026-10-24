package org.entrypulse.entrypulse.service;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.dto.eventtable.CreateEventTableRequest;
import org.entrypulse.entrypulse.model.Event;
import org.entrypulse.entrypulse.model.EventTable;
import org.entrypulse.entrypulse.repository.EventGuestRepository;
import org.entrypulse.entrypulse.repository.EventRepository;
import org.entrypulse.entrypulse.repository.EventTableRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventTableService {

    private final EventTableRepository eventTableRepository;
    private final EventRepository eventRepository;
    private final EventGuestRepository eventGuestRepository;

    private Event requireOwnedEvent(String eventId, String organiserId) {
        return eventRepository
                .findByEventIdAndOrganiser_Id(eventId, organiserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found"));
    }

    public EventTable createEventTable(CreateEventTableRequest request, String organiserId) {
        Event event = requireOwnedEvent(request.eventId(), organiserId);

        if (eventTableRepository.existsByEventEventIdAndTableNumber(
                request.eventId(), request.tableNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A table with this number already exists for this event.");
        }

        EventTable eventTable = new EventTable();
        eventTable.setTableNumber(request.tableNumber());
        eventTable.setCapacity(request.capacity());
        eventTable.setEvent(event);
        return eventTableRepository.save(eventTable);
    }

    public List<EventTable> getTablesForEvent(String eventId, String organiserId) {
        requireOwnedEvent(eventId, organiserId);
        return eventTableRepository.findByEventEventId(eventId);
    }

    public void deleteEventTable(String eventTableId, String organiserId) {
        EventTable table = eventTableRepository.findById(eventTableId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event table not found"));

        if (table.getEvent() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event table not found");
        }
        requireOwnedEvent(table.getEvent().getEventId(), organiserId);

        if (eventGuestRepository.countByEventTable_Id(table.getId()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Move or remove the guests at this table first.");
        }

        eventTableRepository.delete(table);
    }
}