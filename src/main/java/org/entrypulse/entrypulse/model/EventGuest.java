package org.entrypulse.entrypulse.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_event_guest",
                        columnNames = {"event_id", "guest_id"}
                )
        }
)
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventGuest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String eventGuestId;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "event_table_id")
    private EventTable eventTable;

    @ManyToOne
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;


    private String seatNumber;
}
