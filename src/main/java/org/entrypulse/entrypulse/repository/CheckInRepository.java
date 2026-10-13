package org.entrypulse.entrypulse.repository;

import org.entrypulse.entrypulse.model.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckInRepository extends JpaRepository<CheckIn, String> {
        boolean existsByGuestIdAndEventEventId(String guestId, String eventId);
}
