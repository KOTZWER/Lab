# Hotel Booking Management System

REST API на Spring Boot для управления отелями, номерами, гостями, бронированием и платежами.

## Бизнес-правила

1. `Room` всегда принадлежит `Hotel`.
2. `Guest` создаёт `Booking` на интервал дат `[checkInDate, checkOutDate)`.
3. Пересечение броней одного и того же номера запрещено для статусов `PENDING_PAYMENT` и `ACTIVE`.
4. Подтверждённый `Payment` переводит бронь из `PENDING_PAYMENT` в `ACTIVE`.

## Технологии

- Java 21
- Spring Boot 3.5.x
- Spring Data JPA / Hibernate
- Spring Security + JWT (access + refresh)
- PostgreSQL
- Maven

## Сущности

### Hotel
- `id`
- `name`
- `city`
- `address`
- `createdAt`

### Room
- `id`
- `hotel` (FK -> Hotel)
- `roomNumber` (уникален в рамках отеля)
- `type`
- `capacity`
- `pricePerNight`
- `active`
- `createdAt`

### Guest
- `id`
- `fullName`
- `email` (unique)
- `phone` (unique)
- `createdAt`

### Booking
- `id`
- `room` (FK -> Room)
- `guest` (FK -> Guest)
- `checkInDate`
- `checkOutDate`
- `status`: `PENDING_PAYMENT`, `ACTIVE`, `COMPLETED`, `CANCELLED`
- `totalAmount`
- `notes`
- `paymentConfirmedAt`
- `createdAt`
- `updatedAt`

### Payment
- `id`
- `booking` (FK -> Booking)
- `amount`
- `currency`
- `method`
- `transactionReference` (unique)
- `status`: `CONFIRMED`, `REFUNDED`
- `paidAt`
- `createdAt`

## Роли и доступ

- `ROLE_ADMIN`
- `ROLE_MANAGER`
- `ROLE_GUEST`

Ключевые правила доступа:

- `ADMIN/MANAGER`: управление отелями, номерами, просмотр платежей, отчёты.
- `GUEST`: создание гостей, создание/перенос/отмена брони, подтверждение оплаты своей брони (по токену роли).
- `COMPLETE booking`: только `ADMIN` или `MANAGER`.

## Инициализация данных

При первом запуске создаются:

- Пользователи:
  - `admin / Admin1234!`
  - `manager1 / Manager1234!`
  - `guest1 / Guest1234!`
- Демоданные: отели, номера, гости, брони, платежи.

## Запуск

1. Поднять PostgreSQL и создать БД `hotelbookingdb`.
2. Настроить переменные окружения (пример в `.env.example`).
3. Запустить:

```bash
./mvnw spring-boot:run
```

По умолчанию используется HTTP `http://localhost:8080`.

Для HTTPS:

1. Сгенерировать сертификаты `generate-certs.sh` (получить `src/main/resources/keystore.p12`).
2. Установить `SSL_ENABLED=true`.
3. При необходимости установить `SSL_KEY_STORE_PASSWORD`.

## Конфигурация

Основные параметры в `src/main/resources/application.properties`:

- `DB_URL` (default `jdbc:postgresql://localhost:5432/hotelbookingdb`)
- `DB_USERNAME`
- `DB_PASSWORD`
- `SSL_ENABLED`
- `SSL_KEY_STORE_PASSWORD`
- `jwt.secret`

## Аутентификация

### Register
`POST /api/auth/register`

```json
{
  "username": "new_guest",
  "password": "StrongPass1!",
  "role": "ROLE_GUEST"
}
```

### Login
`POST /api/auth/login`

```json
{
  "username": "guest1",
  "password": "Guest1234!"
}
```

Ответ:

```json
{
  "accessToken": "...",
  "refreshToken": "..."
}
```

## Основные API

### Hotels
- `GET /api/hotels`
- `GET /api/hotels/{id}`
- `POST /api/hotels`
- `PUT /api/hotels/{id}`
- `DELETE /api/hotels/{id}`

### Rooms
- `GET /api/rooms`
- `GET /api/rooms/{id}`
- `GET /api/rooms/hotel/{hotelId}`
- `POST /api/rooms`
- `PUT /api/rooms/{id}`
- `DELETE /api/rooms/{id}`

Пример `POST /api/rooms`:

```json
{
  "hotelId": 1,
  "roomNumber": "305",
  "type": "DELUXE",
  "capacity": 2,
  "pricePerNight": 9800,
  "active": true
}
```

### Guests
- `GET /api/guests`
- `GET /api/guests/{id}`
- `POST /api/guests`
- `PUT /api/guests/{id}`
- `DELETE /api/guests/{id}`

### Bookings
- `GET /api/bookings`
- `GET /api/bookings/{id}`
- `GET /api/bookings/guest/{guestId}`
- `GET /api/bookings/room/{roomId}`
- `GET /api/bookings/status/{status}`
- `POST /api/bookings` (создание с проверкой пересечений)
- `PUT /api/bookings/{id}/dates` (перенос дат с повторной проверкой пересечений)
- `PUT /api/bookings/{id}/cancel`
- `PUT /api/bookings/{id}/complete`
- `DELETE /api/bookings/{id}`

Пример `POST /api/bookings`:

```json
{
  "roomId": 1,
  "guestId": 1,
  "checkInDate": "2026-04-10",
  "checkOutDate": "2026-04-13",
  "notes": "Late check-in"
}
```

### Payments
- `GET /api/payments`
- `GET /api/payments/{id}`
- `GET /api/payments/booking/{bookingId}`
- `POST /api/payments/confirm`

Пример `POST /api/payments/confirm`:

```json
{
  "bookingId": 10,
  "amount": 19500,
  "currency": "RUB",
  "method": "CARD",
  "transactionReference": "PAY-2026-0001"
}
```

## Бизнес-операции

1. Создание брони с запретом пересечений: `POST /api/bookings`.
2. Перенос дат брони с запретом пересечений: `PUT /api/bookings/{id}/dates`.
3. Подтверждение платежа и активация брони: `POST /api/payments/confirm`.
4. Поиск свободных номеров по датам: `GET /api/hotels/{hotelId}/available-rooms?checkInDate=...&checkOutDate=...`.
5. Отчёт по загрузке отеля: `GET /api/reports/hotels/{hotelId}/occupancy?fromDate=...&toDate=...`.

## Postman

В репозитории есть готовая коллекция `postman_collection.json` с:

- Auth (register/login/refresh)
- Hotels / Rooms / Guests
- Bookings (включая проверку конфликта по пересечению)
- Payments
- Reports

Перед запуском коллекции:

1. Если включили HTTPS с self-signed сертификатом, отключить SSL verification в Postman.
2. Запустить `Login` запросы для заполнения токенов в переменные коллекции.
