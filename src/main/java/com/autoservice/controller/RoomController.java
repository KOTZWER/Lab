package com.autoservice.controller;

import com.autoservice.domain.Hotel;
import com.autoservice.domain.Room;
import com.autoservice.dto.RoomUpsertRequest;
import com.autoservice.repository.HotelRepository;
import com.autoservice.repository.RoomRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;

    public RoomController(RoomRepository roomRepository, HotelRepository hotelRepository) {
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
    }

    @GetMapping
    public List<Room> getAll() {
        return roomRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> getById(@PathVariable Long id) {
        return roomRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/hotel/{hotelId}")
    public List<Room> getByHotel(@PathVariable Long hotelId) {
        return roomRepository.findByHotelId(hotelId);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody RoomUpsertRequest request) {
        Hotel hotel = hotelRepository.findById(request.getHotelId()).orElse(null);
        if (hotel == null) {
            return ResponseEntity.badRequest().body("Hotel not found");
        }

        if (roomRepository.existsByHotelIdAndRoomNumber(request.getHotelId(), request.getRoomNumber())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Room number already exists in this hotel");
        }

        Room room = new Room();
        room.setHotel(hotel);
        room.setRoomNumber(request.getRoomNumber());
        room.setType(request.getType());
        room.setCapacity(request.getCapacity());
        room.setPricePerNight(request.getPricePerNight());
        room.setActive(Boolean.TRUE.equals(request.getActive()));

        return ResponseEntity.status(HttpStatus.CREATED).body(roomRepository.save(room));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody RoomUpsertRequest request) {
        Hotel hotel = hotelRepository.findById(request.getHotelId()).orElse(null);
        if (hotel == null) {
            return ResponseEntity.badRequest().body("Hotel not found");
        }

        return roomRepository.findById(id)
                .map(existing -> {
                    boolean numberChanged = !existing.getRoomNumber().equals(request.getRoomNumber())
                            || !existing.getHotel().getId().equals(request.getHotelId());
                    if (numberChanged && roomRepository.existsByHotelIdAndRoomNumber(request.getHotelId(), request.getRoomNumber())) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body("Room number already exists in this hotel");
                    }

                    existing.setHotel(hotel);
                    existing.setRoomNumber(request.getRoomNumber());
                    existing.setType(request.getType());
                    existing.setCapacity(request.getCapacity());
                    existing.setPricePerNight(request.getPricePerNight());
                    existing.setActive(Boolean.TRUE.equals(request.getActive()));
                    return ResponseEntity.ok(roomRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!roomRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        roomRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
