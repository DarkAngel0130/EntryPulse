package org.entrypulse.entrypulse.repository;

import org.entrypulse.entrypulse.model.EventGuest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventGuestRepository extends JpaRepository<EventGuest, String> {

    List<EventGuest> findByEventEventId(String eventId);
}
