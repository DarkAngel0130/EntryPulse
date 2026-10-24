package org.entrypulse.entrypulse.repository;

import org.entrypulse.entrypulse.model.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CheckInRepository extends JpaRepository<CheckIn, String> {

        boolean existsByGuest_IdAndEvent_EventId(
                String guestId,
                String eventId
        );

        long countByGuest_IdAndEvent_EventId(
                String guestId,
                String eventId
        );

        long countByEventEventId(String eventId);

        long countByEvent_EventIdAndCheckedInBy_Id(
                String eventId,
                String securityUserId
        );

        List<CheckIn> findByEvent_EventIdOrderByCheckInTimeDesc(
                String eventId
        );

        List<CheckIn> findByEvent_EventIdAndCheckedInBy_IdOrderByCheckInTimeDesc(
                String eventId,
                String securityUserId
        );

        Optional<CheckIn> findByGuest_IdAndEvent_EventId(
                String guestId,
                String eventId
        );
}