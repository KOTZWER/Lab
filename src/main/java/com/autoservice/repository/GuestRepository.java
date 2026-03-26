package com.autoservice.repository;

import com.autoservice.domain.Guest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, Long> {
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
