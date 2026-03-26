package com.autoservice.controller;

import com.autoservice.domain.Hotel;
import com.autoservice.repository.HotelRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    private final HotelRepository hotelRepository;

    public HotelController(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @GetMapping
    public List<Hotel> getAll() {
        return hotelRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Hotel> getById(@PathVariable Long id) {
        return hotelRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Hotel hotel) {
        if (hotelRepository.existsByNameAndAddress(hotel.getName(), hotel.getAddress())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Hotel with same name and address already exists");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(hotelRepository.save(hotel));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Hotel updated) {
        return hotelRepository.findById(id)
                .map(existing -> {
                    existing.setName(updated.getName());
                    existing.setCity(updated.getCity());
                    existing.setAddress(updated.getAddress());
                    return ResponseEntity.ok(hotelRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!hotelRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        hotelRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
