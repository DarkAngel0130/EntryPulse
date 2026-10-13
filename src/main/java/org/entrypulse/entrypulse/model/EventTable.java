package org.entrypulse.entrypulse.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventTable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private int tableNumber;

    @Min(value = 1, message = "Capacity must at least be one")
    private int capacity;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;
}
