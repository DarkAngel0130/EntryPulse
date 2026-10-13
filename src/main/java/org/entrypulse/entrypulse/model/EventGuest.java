package org.entrypulse.entrypulse.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @JoinColumn(name = "event_table_id", nullable = false)
    private EventTable eventTable;

    @ManyToOne
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;


    private String seatNumber;
}
