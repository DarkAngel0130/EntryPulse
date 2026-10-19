package org.entrypulse.entrypulse.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Table(
        uniqueConstraints = @UniqueConstraint(
                name = "uk_checkin_guest_event",
                columnNames = {"guest_id", "event_event_id"}
        )
)
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private LocalDateTime checkInTime;

    @ManyToOne
    private Guest guest;

    @ManyToOne
    private Event event;

    @ManyToOne
    private User checkedInBy;
}
