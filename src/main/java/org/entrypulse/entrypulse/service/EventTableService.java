
package org.entrypulse.entrypulse.service;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.dto.eventtable.CreateEventTableRequest;
import org.entrypulse.entrypulse.model.Event;
import org.entrypulse.entrypulse.model.EventTable;
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

    public EventTable createEventTable(
            CreateEventTableRequest request) {

        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"
                ));

        if (eventTableRepository
                .existsByEventEventIdAndTableNumber(
                        request.eventId(),
                        request.tableNumber())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A table with this number already exists for this event."
            );
        }

        EventTable eventTable = new EventTable();
        eventTable.setTableNumber(request.tableNumber());
        eventTable.setCapacity(request.capacity());
        eventTable.setEvent(event);

        return eventTableRepository.save(eventTable);
    }

    public List<EventTable> getTablesForEvent(String eventId) {

        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Event not found"
            );
        }

        return eventTableRepository.findByEventEventId(eventId);
    }


    public void deleteEventTable(String eventTableId) {
        System.out.println("DELETE SERVICE REACHED: " + eventTableId);

        EventTable eventTable = eventTableRepository
                .findById(eventTableId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event table not found"
                ));

        System.out.println("TABLE FOUND: " + eventTable.getId());

        eventTableRepository.delete(eventTable);

        System.out.println("DELETE OPERATION COMPLETED");
    }

}
