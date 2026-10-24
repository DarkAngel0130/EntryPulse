
package org.entrypulse.entrypulse.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventSecurity {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public enum AssignmentStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        REVOKED
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status = AssignmentStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    private LocalDateTime respondedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = AssignmentStatus.PENDING;
        }

        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }
}
