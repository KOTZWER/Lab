package com.autoservice;

import com.autoservice.domain.*;
import com.autoservice.repository.*;
import com.autoservice.security.AppUser;
import com.autoservice.security.AppUserRepository;
import com.autoservice.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(HotelRepository hotelRepository,
                           RoomRepository roomRepository,
                           GuestRepository guestRepository,
                           BookingRepository bookingRepository,
                           PaymentRepository paymentRepository,
                           AppUserRepository appUserRepository,
                           PasswordEncoder passwordEncoder) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.guestRepository = guestRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (hotelRepository.count() > 0) {
            log.info("Database already seeded, skipping initialization.");
            return;
        }

        createUserIfAbsent("admin", "Admin1234!", Role.ROLE_ADMIN);
        createUserIfAbsent("manager1", "Manager1234!", Role.ROLE_MANAGER);
        createUserIfAbsent("guest1", "Guest1234!", Role.ROLE_GUEST);

        Hotel grand = createHotel("Grand Aurora", "Moscow", "Tverskaya St, 12");
        Hotel riviera = createHotel("Nevsky Riviera", "Saint Petersburg", "Nevsky Ave, 24");

        Room g101 = createRoom(grand, "101", "STANDARD", 2, new BigDecimal("6500.00"), true);
        Room g102 = createRoom(grand, "102", "DELUXE", 2, new BigDecimal("9200.00"), true);
        Room g201 = createRoom(grand, "201", "SUITE", 4, new BigDecimal("14000.00"), true);

        Room r301 = createRoom(riviera, "301", "STANDARD", 2, new BigDecimal("5900.00"), true);
        Room r302 = createRoom(riviera, "302", "FAMILY", 4, new BigDecimal("11000.00"), true);

        Guest ivan = createGuest("Ivan Petrov", "ivan.petrov@example.com", "+7-900-111-2233");
        Guest anna = createGuest("Anna Sokolova", "anna.sokolova@example.com", "+7-900-444-5566");

        LocalDate today = LocalDate.now();

        Booking pendingBooking = createBooking(
                g101,
                ivan,
                today.plusDays(4),
                today.plusDays(7),
                BookingStatus.PENDING_PAYMENT,
                "Late check-in requested",
                null
        );

        Booking activeBooking = createBooking(
                g102,
                anna,
                today.plusDays(1),
                today.plusDays(4),
                BookingStatus.ACTIVE,
                "Airport transfer included",
                LocalDateTime.now().minusDays(1)
        );

        createPayment(activeBooking,
                activeBooking.getTotalAmount(),
                "RUB",
                "CARD",
                "PAY-INIT-1001",
                PaymentStatus.CONFIRMED,
                LocalDateTime.now().minusDays(1));

        createBooking(
                r301,
                ivan,
                today.plusDays(10),
                today.plusDays(13),
                BookingStatus.CANCELLED,
                "Cancelled by guest",
                null
        );

        Booking completedBooking = createBooking(
                r302,
                anna,
                today.minusDays(7),
                today.minusDays(4),
                BookingStatus.COMPLETED,
                "Business trip",
                today.minusDays(8).atStartOfDay()
        );

        createPayment(completedBooking,
                completedBooking.getTotalAmount(),
                "RUB",
                "CARD",
                "PAY-INIT-1002",
                PaymentStatus.CONFIRMED,
                today.minusDays(8).atStartOfDay());

        log.info("Database seeded: {} hotels, {} rooms, {} guests, {} bookings, {} payments",
                hotelRepository.count(),
                roomRepository.count(),
                guestRepository.count(),
                bookingRepository.count(),
                paymentRepository.count());
    }

    private void createUserIfAbsent(String username, String rawPassword, Role role) {
        if (!appUserRepository.existsByUsername(username)) {
            AppUser user = new AppUser();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            appUserRepository.save(user);
        }
    }

    private Hotel createHotel(String name, String city, String address) {
        Hotel hotel = new Hotel();
        hotel.setName(name);
        hotel.setCity(city);
        hotel.setAddress(address);
        return hotelRepository.save(hotel);
    }

    private Room createRoom(Hotel hotel,
                            String roomNumber,
                            String type,
                            Integer capacity,
                            BigDecimal pricePerNight,
                            boolean active) {
        Room room = new Room();
        room.setHotel(hotel);
        room.setRoomNumber(roomNumber);
        room.setType(type);
        room.setCapacity(capacity);
        room.setPricePerNight(pricePerNight);
        room.setActive(active);
        return roomRepository.save(room);
    }

    private Guest createGuest(String fullName, String email, String phone) {
        Guest guest = new Guest();
        guest.setFullName(fullName);
        guest.setEmail(email);
        guest.setPhone(phone);
        return guestRepository.save(guest);
    }

    private Booking createBooking(Room room,
                                  Guest guest,
                                  LocalDate checkInDate,
                                  LocalDate checkOutDate,
                                  BookingStatus status,
                                  String notes,
                                  LocalDateTime paymentConfirmedAt) {
        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setGuest(guest);
        booking.setCheckInDate(checkInDate);
        booking.setCheckOutDate(checkOutDate);
        booking.setStatus(status);
        booking.setNotes(notes);
        booking.setPaymentConfirmedAt(paymentConfirmedAt);
        booking.setTotalAmount(room.getPricePerNight().multiply(
                BigDecimal.valueOf(checkOutDate.toEpochDay() - checkInDate.toEpochDay())));
        return bookingRepository.save(booking);
    }

    private Payment createPayment(Booking booking,
                                  BigDecimal amount,
                                  String currency,
                                  String method,
                                  String transactionReference,
                                  PaymentStatus status,
                                  LocalDateTime paidAt) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setMethod(method);
        payment.setTransactionReference(transactionReference);
        payment.setStatus(status);
        payment.setPaidAt(paidAt);
        return paymentRepository.save(payment);
    }
}
