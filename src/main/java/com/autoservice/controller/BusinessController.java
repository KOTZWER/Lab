package com.autoservice.controller;

import com.autoservice.dto.HotelOccupancyDto;
import com.autoservice.dto.RoomAvailabilityDto;
import com.autoservice.service.BusinessService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @GetMapping("/hotels/{hotelId}/available-rooms")
    public ResponseEntity<?> getAvailableRooms(@PathVariable Long hotelId,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                               LocalDate checkInDate,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                               LocalDate checkOutDate) {
        try {
            List<RoomAvailabilityDto> rooms = businessService.findAvailableRooms(hotelId, checkInDate, checkOutDate);
            return ResponseEntity.ok(rooms);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/reports/hotels/{hotelId}/occupancy")
    public ResponseEntity<?> getHotelOccupancy(@PathVariable Long hotelId,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                               LocalDate fromDate,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                               LocalDate toDate) {
        try {
            HotelOccupancyDto report = businessService.getHotelOccupancy(hotelId, fromDate, toDate);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
