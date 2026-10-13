package org.entrypulse.entrypulse.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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
