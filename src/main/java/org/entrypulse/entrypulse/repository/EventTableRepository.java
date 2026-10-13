package org.entrypulse.entrypulse.repository;

import org.entrypulse.entrypulse.model.Event;
import org.entrypulse.entrypulse.model.EventTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventTableRepository extends JpaRepository<EventTable, String> {
    List<EventTable> findByEventEventId(String eventId);
}
