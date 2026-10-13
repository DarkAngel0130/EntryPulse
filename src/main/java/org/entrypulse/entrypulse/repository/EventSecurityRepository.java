package org.entrypulse.entrypulse.repository;

import org.entrypulse.entrypulse.model.EventSecurity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventSecurityRepository extends JpaRepository<EventSecurity, String> {
    List<EventSecurity> findByEventEventId(String eventId);

    List<EventSecurity> findByUserId(String userId);

    boolean existsByEventEventIdAndUserId(String eventId, String userId);
}
