package org.entrypulse.entrypulse.repository;

import org.entrypulse.entrypulse.model.Guest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, String> {
}
