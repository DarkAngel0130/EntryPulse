package org.entrypulse.entrypulse.repository;

import org.entrypulse.entrypulse.model.Event;
import org.entrypulse.entrypulse.model.EventGuest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, String> {
}
