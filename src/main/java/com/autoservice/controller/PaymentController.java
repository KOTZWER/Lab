package com.autoservice.controller;

import com.autoservice.domain.Payment;
import com.autoservice.dto.PaymentConfirmRequest;
import com.autoservice.repository.PaymentRepository;
import com.autoservice.service.BusinessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final BusinessService businessService;

    public PaymentController(PaymentRepository paymentRepository, BusinessService businessService) {
        this.paymentRepository = paymentRepository;
        this.businessService = businessService;
    }

    @GetMapping
    public List<Payment> getAll() {
        return paymentRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getById(@PathVariable Long id) {
        return paymentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/booking/{bookingId}")
    public List<Payment> getByBooking(@PathVariable Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@Valid @RequestBody PaymentConfirmRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(businessService.confirmPayment(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }
}
