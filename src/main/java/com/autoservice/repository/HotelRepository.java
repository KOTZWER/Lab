package com.autoservice.repository;

import com.autoservice.domain.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    boolean existsByNameAndAddress(String name, String address);
}
