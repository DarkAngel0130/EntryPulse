
package org.entrypulse.entrypulse.repository;

import org.entrypulse.entrypulse.model.EventSecurity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventSecurityRepository
        extends JpaRepository<EventSecurity, String> {

    List<EventSecurity> findByEvent_EventIdAndEvent_Organiser_Id(
            String eventId,
            String organiserId
    );

    List<EventSecurity> findByUser_Id(String userId);

    Optional<EventSecurity> findByIdAndUser_Id(
            String assignmentId,
            String userId
    );

    Optional<EventSecurity> findByIdAndEvent_Organiser_Id(
            String assignmentId,
            String organiserId
    );

    boolean existsByEvent_EventIdAndUser_Id(
            String eventId,
            String userId
    );


    boolean existsByEvent_EventIdAndUser_IdAndStatus(
            String eventId,
            String userId,
            EventSecurity.AssignmentStatus status
    );

}
