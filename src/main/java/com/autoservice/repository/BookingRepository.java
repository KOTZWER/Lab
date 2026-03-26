package com.autoservice.repository;

import com.autoservice.domain.Booking;
import com.autoservice.domain.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByRoomId(Long roomId);

    List<Booking> findByGuestId(Long guestId);

    List<Booking> findByStatus(BookingStatus status);

    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
            FROM Booking b
            WHERE b.room.id = :roomId
              AND b.status IN ('PENDING_PAYMENT', 'ACTIVE')
              AND :checkInDate < b.checkOutDate
              AND :checkOutDate > b.checkInDate
            """)
    boolean existsOverlappingBooking(@Param("roomId") Long roomId,
                                     @Param("checkInDate") LocalDate checkInDate,
                                     @Param("checkOutDate") LocalDate checkOutDate);

    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
            FROM Booking b
            WHERE b.room.id = :roomId
              AND b.id <> :bookingId
              AND b.status IN ('PENDING_PAYMENT', 'ACTIVE')
              AND :checkInDate < b.checkOutDate
              AND :checkOutDate > b.checkInDate
            """)
    boolean existsOverlappingBookingExcludingBooking(@Param("roomId") Long roomId,
                                                     @Param("bookingId") Long bookingId,
                                                     @Param("checkInDate") LocalDate checkInDate,
                                                     @Param("checkOutDate") LocalDate checkOutDate);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.room.hotel.id = :hotelId
              AND b.status IN ('ACTIVE', 'COMPLETED')
              AND :fromDate < b.checkOutDate
              AND :toDate > b.checkInDate
            """)
    List<Booking> findBookingsForOccupancy(@Param("hotelId") Long hotelId,
                                           @Param("fromDate") LocalDate fromDate,
                                           @Param("toDate") LocalDate toDate);
}
