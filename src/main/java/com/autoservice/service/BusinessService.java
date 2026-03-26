package com.autoservice.service;

import com.autoservice.domain.*;
import com.autoservice.dto.*;
import com.autoservice.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BusinessService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    public BusinessService(HotelRepository hotelRepository,
                           RoomRepository roomRepository,
                           GuestRepository guestRepository,
                           BookingRepository bookingRepository,
                           PaymentRepository paymentRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.guestRepository = guestRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Booking createBooking(BookingCreateRequest request) {
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + request.getRoomId()));
        Guest guest = guestRepository.findById(request.getGuestId())
                .orElseThrow(() -> new IllegalArgumentException("Guest not found: " + request.getGuestId()));

        if (!room.isActive()) {
            throw new IllegalStateException("Room is inactive and cannot be booked");
        }

        validateDateRange(request.getCheckInDate(), request.getCheckOutDate());

        boolean overlap = bookingRepository.existsOverlappingBooking(
                room.getId(), request.getCheckInDate(), request.getCheckOutDate());
        if (overlap) {
            throw new IllegalStateException("Booking dates overlap with another booking for this room");
        }

        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setGuest(guest);
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setNotes(request.getNotes());
        booking.setTotalAmount(calculateBookingAmount(room.getPricePerNight(), request.getCheckInDate(), request.getCheckOutDate()));

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking changeBookingDates(Long bookingId, BookingDateChangeRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Only PENDING_PAYMENT bookings can be rescheduled");
        }

        validateDateRange(request.getCheckInDate(), request.getCheckOutDate());

        boolean overlap = bookingRepository.existsOverlappingBookingExcludingBooking(
                booking.getRoom().getId(), bookingId, request.getCheckInDate(), request.getCheckOutDate());
        if (overlap) {
            throw new IllegalStateException("Updated dates overlap with another booking for this room");
        }

        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setTotalAmount(calculateBookingAmount(
                booking.getRoom().getPricePerNight(), request.getCheckInDate(), request.getCheckOutDate()));

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Completed booking cannot be cancelled");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return booking;
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking completeBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE bookings can be completed");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        return bookingRepository.save(booking);
    }

    @Transactional
    public Payment confirmPayment(PaymentConfirmRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + request.getBookingId()));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Payment can only confirm booking in PENDING_PAYMENT status");
        }

        if (paymentRepository.existsByBookingIdAndStatus(booking.getId(), PaymentStatus.CONFIRMED)) {
            throw new IllegalStateException("Booking already has a confirmed payment");
        }

        if (request.getAmount().compareTo(booking.getTotalAmount()) != 0) {
            throw new IllegalStateException("Payment amount must match booking totalAmount");
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency().toUpperCase());
        payment.setMethod(request.getMethod());
        payment.setTransactionReference(request.getTransactionReference());
        payment.setStatus(PaymentStatus.CONFIRMED);
        payment.setPaidAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        booking.setStatus(BookingStatus.ACTIVE);
        booking.setPaymentConfirmedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        return savedPayment;
    }

    @Transactional(readOnly = true)
    public List<RoomAvailabilityDto> findAvailableRooms(Long hotelId, LocalDate checkInDate, LocalDate checkOutDate) {
        validateDateRange(checkInDate, checkOutDate);

        if (!hotelRepository.existsById(hotelId)) {
            throw new IllegalArgumentException("Hotel not found: " + hotelId);
        }

        return roomRepository.findAvailableRooms(hotelId, checkInDate, checkOutDate)
                .stream()
                .map(room -> new RoomAvailabilityDto(
                        room.getId(),
                        room.getRoomNumber(),
                        room.getType(),
                        room.getCapacity(),
                        room.getPricePerNight()))
                .toList();
    }

    @Transactional(readOnly = true)
    public HotelOccupancyDto getHotelOccupancy(Long hotelId, LocalDate fromDate, LocalDate toDate) {
        validateDateRange(fromDate, toDate);

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new IllegalArgumentException("Hotel not found: " + hotelId));

        long rangeDays = ChronoUnit.DAYS.between(fromDate, toDate);
        long activeRooms = roomRepository.countByHotelIdAndActiveTrue(hotelId);
        long totalRoomNights = activeRooms * rangeDays;

        long occupiedRoomNights = bookingRepository.findBookingsForOccupancy(hotelId, fromDate, toDate)
                .stream()
                .mapToLong(booking -> calculateOverlapDays(
                        booking.getCheckInDate(), booking.getCheckOutDate(), fromDate, toDate))
                .sum();

        BigDecimal occupancyPercent = totalRoomNights == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(occupiedRoomNights)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalRoomNights), 2, RoundingMode.HALF_UP);

        return new HotelOccupancyDto(
                hotel.getId(),
                hotel.getName(),
                fromDate,
                toDate,
                totalRoomNights,
                occupiedRoomNights,
                occupancyPercent);
    }

    private BigDecimal calculateBookingAmount(BigDecimal pricePerNight, LocalDate checkInDate, LocalDate checkOutDate) {
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        return pricePerNight.multiply(BigDecimal.valueOf(nights));
    }

    private long calculateOverlapDays(LocalDate bookingStart,
                                      LocalDate bookingEnd,
                                      LocalDate reportStart,
                                      LocalDate reportEnd) {
        LocalDate start = bookingStart.isAfter(reportStart) ? bookingStart : reportStart;
        LocalDate end = bookingEnd.isBefore(reportEnd) ? bookingEnd : reportEnd;
        if (!start.isBefore(end)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    private void validateDateRange(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalArgumentException("Both checkInDate and checkOutDate are required");
        }
        if (!checkInDate.isBefore(checkOutDate)) {
            throw new IllegalArgumentException("checkInDate must be before checkOutDate");
        }
    }
}
