package org.entrypulse.entrypulse.repository;

import org.entrypulse.entrypulse.model.Event;
import org.entrypulse.entrypulse.model.EventGuest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, String> {
    List<Event> findByOrganiser_Id(String organiserId);

    Optional<Event> findByEventIdAndOrganiser_Id(
            String eventId,
            String organiserId
    );
}
