package org.entrypulse.entrypulse.service;


import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.model.EventTable;
import org.entrypulse.entrypulse.repository.EventTableRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventTableService {
    private final EventTableRepository eventTableRepository;


    public EventTable createEventTable(EventTable eventTable) {

        return eventTableRepository.save(eventTable);
    }

    public List<EventTable> getTablesForEvent(String eventId) {

        return eventTableRepository.findByEventEventId(eventId);
    }
}
