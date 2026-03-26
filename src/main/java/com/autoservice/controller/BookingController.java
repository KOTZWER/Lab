package com.autoservice.controller;

import com.autoservice.domain.Booking;
import com.autoservice.domain.BookingStatus;
import com.autoservice.dto.BookingCreateRequest;
import com.autoservice.dto.BookingDateChangeRequest;
import com.autoservice.repository.BookingRepository;
import com.autoservice.repository.PaymentRepository;
import com.autoservice.service.BusinessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BusinessService businessService;

    public BookingController(BookingRepository bookingRepository,
                             PaymentRepository paymentRepository,
                             BusinessService businessService) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.businessService = businessService;
    }

    @GetMapping
    public List<Booking> getAll() {
        return bookingRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> getById(@PathVariable Long id) {
        return bookingRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/guest/{guestId}")
    public List<Booking> getByGuest(@PathVariable Long guestId) {
        return bookingRepository.findByGuestId(guestId);
    }

    @GetMapping("/room/{roomId}")
    public List<Booking> getByRoom(@PathVariable Long roomId) {
        return bookingRepository.findByRoomId(roomId);
    }

    @GetMapping("/status/{status}")
    public List<Booking> getByStatus(@PathVariable BookingStatus status) {
        return bookingRepository.findByStatus(status);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody BookingCreateRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(businessService.createBooking(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/dates")
    public ResponseEntity<?> changeDates(@PathVariable Long id,
                                         @Valid @RequestBody BookingDateChangeRequest request) {
        try {
            return ResponseEntity.ok(businessService.changeBookingDates(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(businessService.cancelBooking(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<?> complete(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(businessService.completeBooking(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!bookingRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (paymentRepository.countByBookingId(id) > 0) {
            return ResponseEntity.badRequest().body("Cannot delete booking with payments");
        }
        bookingRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
