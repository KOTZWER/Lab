package com.autoservice.repository;

import com.autoservice.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByHotelId(Long hotelId);

    boolean existsByHotelIdAndRoomNumber(Long hotelId, String roomNumber);

    long countByHotelIdAndActiveTrue(Long hotelId);

    @Query("""
            SELECT r FROM Room r
            WHERE r.hotel.id = :hotelId
              AND r.active = true
              AND r.id NOT IN (
                  SELECT b.room.id FROM Booking b
                  WHERE b.status IN ('PENDING_PAYMENT', 'ACTIVE')
                    AND :checkInDate < b.checkOutDate
                    AND :checkOutDate > b.checkInDate
              )
            ORDER BY r.roomNumber
            """)
    List<Room> findAvailableRooms(@Param("hotelId") Long hotelId,
                                  @Param("checkInDate") LocalDate checkInDate,
                                  @Param("checkOutDate") LocalDate checkOutDate);
}
