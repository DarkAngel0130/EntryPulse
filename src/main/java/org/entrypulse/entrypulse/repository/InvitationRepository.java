
package org.entrypulse.entrypulse.repository;

import org.entrypulse.entrypulse.model.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository
        extends JpaRepository<Invitation, String> {

    Optional<Invitation> findByToken(String token);

    Optional<Invitation> findByIdAndEvent_Organiser_Id(
            String invitationId,
            String organiserId
    );

    boolean existsByEventGuest_EventGuestId(String eventGuestId);

    long countByEvent_EventId(String eventId);


    List<Invitation> findByEvent_EventIdAndEvent_Organiser_Id(
            String eventId,
            String organiserId
    );

}
