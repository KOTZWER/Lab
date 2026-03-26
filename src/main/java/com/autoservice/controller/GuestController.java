package com.autoservice.controller;

import com.autoservice.domain.Guest;
import com.autoservice.repository.GuestRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guests")
public class GuestController {

    private final GuestRepository guestRepository;

    public GuestController(GuestRepository guestRepository) {
        this.guestRepository = guestRepository;
    }

    @GetMapping
    public List<Guest> getAll() {
        return guestRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Guest> getById(@PathVariable Long id) {
        return guestRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Guest guest) {
        if (guest.getEmail() != null && guestRepository.existsByEmail(guest.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
        }
        if (guest.getPhone() != null && guestRepository.existsByPhone(guest.getPhone())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Phone already exists");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(guestRepository.save(guest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Guest updated) {
        return guestRepository.findById(id)
                .map(existing -> {
                    if (updated.getEmail() != null
                            && !updated.getEmail().equals(existing.getEmail())
                            && guestRepository.existsByEmail(updated.getEmail())) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
                    }
                    if (updated.getPhone() != null
                            && !updated.getPhone().equals(existing.getPhone())
                            && guestRepository.existsByPhone(updated.getPhone())) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body("Phone already exists");
                    }

                    existing.setFullName(updated.getFullName());
                    existing.setEmail(updated.getEmail());
                    existing.setPhone(updated.getPhone());
                    return ResponseEntity.ok(guestRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!guestRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        guestRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
