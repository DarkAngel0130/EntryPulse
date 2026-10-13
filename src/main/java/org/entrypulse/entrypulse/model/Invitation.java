package org.entrypulse.entrypulse.model;

import org.entrypulse.entrypulse.model.Event;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Invitation {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    private InvitationStatus status;

    @ManyToOne
    private Guest guest;

    @ManyToOne
    private Event event;

    @ManyToOne
    @JoinColumn(name = "event_guest_id", nullable = false)
    private EventGuest eventGuest;

    public enum InvitationStatus {
        ACTIVE,
        USED,
        CANCELLED
    }
}
