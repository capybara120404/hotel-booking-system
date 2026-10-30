# Hotel Booking System

Распределённая система бронирования отелей на базе Spring Boot и микросервисной архитектуры, реализует двухшаговое подтверждение бронирований (сага с компенсацией), идемпотентность операций, сквозную трассировку и разграничение доступа по ролям.

## Архитектура

Система состоит из четырёх независимых Spring Boot приложений, взаимодействующих через REST и регистрирующихся в Eureka:

- **Eureka Server** (порт 8761) - реестр сервисов, обеспечивает service discovery.
- **API Gateway** (порт 8080) - единая точка входа, маршрутизация, валидация JWT, генерация `X-Request-Id`, блокировка внутренних маршрутов.
- **Hotel Service** (порт 8082) - управление отелями и номерами, временные блокировки слотов, статистика `times_booked`.
- **Booking Service** (порт 8083) - регистрация/авторизация пользователей, создание бронирований, оркестрация саги, автоподбор комнат.

Поток запроса: `Client -> Gateway -> (Eureka lookup) -> Target Service`.

Межсервисные вызовы: `Booking Service -> Hotel Service` через `RestClient` + `@LoadBalanced`.

## Технологии

- Java 17+
- Spring Boot 3.5.0
- Spring Cloud 2025.0.0
- Spring Data JPA + H2 (in-memory)
- Spring Security + OAuth2 Resource Server / Custom JWT Filter + JWT (JJWT 0.12.6)
- Spring Cloud Gateway
- Netflix Eureka
- Resilience4j (Retry + CircuitBreaker)
- MapStruct
- Lombok
- SpringDoc OpenAPI
- JUnit 5, Mockito, MockMvc, TestRestTemplate
- Gradle 9.6
- Spotless (форматирование)

## Предварительные требования

- JDK 17+
- Переменная окружения `JWT_SECRET` (минимум 32 байта)

## Запуск

```bash
export JWT_SECRET="your-32-bytes-secret-key-here!!"

./gradlew :eureka-server:bootRun &
./gradlew :api-gateway:bootRun &
./gradlew :hotel-service:bootRun &
./gradlew :booking-service:bootRun &
```

Порядок важен: сначала Eureka, затем остальные сервисы.

Swagger UI доступен по адресу `http://localhost:8080/swagger-ui.html`.

## Аутентификация

Публичные эндпойнты:

- `POST /user/register`
- `POST /user/auth`

Токен передаётся в заголовке `Authorization: Bearer <token>`, срок действия - 1 час.

### Регистрация

```bash
curl -X POST http://localhost:8080/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"secret123"}'
```

### Вход

```bash
curl -X POST http://localhost:8080/user/auth \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"secret123"}'
```

Предзаполненные учётные записи (в профиле `dev`):

- `admin/admin123` (ADMIN)
- `user/user123` (USER)

## API

### Booking Service (через Gateway)

| Метод | Путь | Роль | Описание |
| --- | --- | --- | --- |
| POST | `/user/register` | PUBLIC | Регистрация |
| POST | `/user/auth` | PUBLIC | Авторизация |
| POST | `/api/bookings` | USER | Создать бронирование (заголовок `Idempotency-Key` опционален) |
| GET | `/api/bookings` | USER | История бронирований (пагинация) |
| GET | `/api/bookings/{id}` | USER | Бронирование по ID |
| DELETE | `/api/bookings/{id}` | USER | Отменить своё бронирование |
| POST | `/user` | ADMIN | Создать пользователя |
| PATCH | `/user/{id}` | ADMIN | Обновить пользователя |
| DELETE | `/user/{id}` | ADMIN | Удалить пользователя |

### Hotel Service (через Gateway)

| Метод | Путь | Роль | Описание |
| --- | --- | --- | --- |
| POST | `/api/hotels` | ADMIN | Создать отель |
| GET | `/api/hotels` | USER/ADMIN | Список отелей (фильтр `?name=`) |
| GET | `/api/hotels/{id}` | USER/ADMIN | Отель по ID |
| PUT | `/api/hotels/{id}` | ADMIN | Обновить отель |
| DELETE | `/api/hotels/{id}` | ADMIN | Удалить отель (без номеров) |
| GET | `/api/hotels/occupancy-stats` | ADMIN | Статистика загруженности номеров |
| POST | `/api/rooms` | ADMIN | Создать номер |
| GET | `/api/rooms?startDate=&endDate=` | USER/ADMIN | Свободные номера |
| GET | `/api/rooms/recommend?startDate=&endDate=` | USER/ADMIN | Рекомендованные (сортировка по `times_booked`, затем `id`) |
| PUT | `/api/rooms/{id}` | ADMIN | Обновить номер |
| DELETE | `/api/rooms/{id}` | ADMIN | Удалить номер |

Внутренние эндпойнты Hotel Service (`/api/rooms/{id}/confirm-availability`, `/api/rooms/{id}/release`, `/api/rooms/{id}/commit`) заблокированы на Gateway фильтром `InternalRouteBlockFilter`.

## Пример создания бронирования

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: unique-key-123" \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": 1,
    "startDate": "2026-11-10",
    "endDate": "2026-11-15",
    "autoSelect": false
  }'
```

### Автоподбор комнаты

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "startDate": "2026-11-10",
    "endDate": "2026-11-15",
    "autoSelect": true
  }'
```

## Тестирование

```bash
./gradlew test
```

Покрытие:

- Юнит-тесты сервисов (`AuthServiceTest`, `BookingOrchestratorTest`, `RoomServiceTest`)
- Контроллеры через MockMvc (`BookingControllerTest`, `RoomControllerTest`)
- Интеграционные тесты саги, компенсации, идемпотентности и параллельных бронирований (`BookingIntegrationTest`, `RoomConcurrencyIntegrationTest`)

## Структура баз данных

### Booking Service

- `users` (id, username, password, role)
- `bookings` (id, user_id, room_id, start_date, end_date, status, request_id, idempotency_key, created_at)
- Уникальные индексы: `request_id`, `(user_id, idempotency_key)`

### Hotel Service

- `hotels` (id, name, address)
- `rooms` (id, hotel_id, number, available, times_booked, version)
- Уникальный индекс: `(hotel_id, number)`
- Индекс: `hotel_id`
- Optimistic locking через `@Version`
- `room_locks` (id, room_id, start_date, end_date, request_id, created_at, expires_at, status)
- Уникальный индекс: `request_id`
- Индекс: `(room_id, start_date, end_date)`

## ADR (Architecture Decision Records)

### ADR-1: Сага с компенсацией вместо 2PC

**Контекст.** Бронирование требует согласованности между Booking и Hotel сервисами.

**Решение.** Использовать паттерн Saga: локальная транзакция PENDING -> удалённый вызов confirm-availability -> CONFIRMED/CANCELLED. При сбое - компенсация через `/release`.

**Обоснование.** 2PC блокирует ресурсы и плохо масштабируется; сага сохраняет доступность и соответствует природе микросервисов.

### ADR-2: Идемпотентность через `Idempotency-Key` и `requestId`

**Контекст.** Повторные запросы (retry, таймауты) не должны создавать дубликаты.

**Решение.** Клиент передаёт `Idempotency-Key`; сервис генерирует внутренний `requestId`, который используется как ключ блокировки в Hotel Service.

**Обоснование.** Уникальные ограничения в БД + проверка в сервисе гарантируют, что повторный вызов возвращает существующий результат или переиспользует отменённую бронь при автоподборе.

### ADR-3: Временные блокировки слотов (`room_locks`)

**Контекст.** Параллельные бронирования одного номера на пересекающиеся даты.

**Решение.** Отдельная таблица `room_locks` с pessimistic locking (`findByIdForUpdate`) и проверкой пересечения дат на уровне SQL.

**Обоснование.** Отделение временных блокировок от сущности `Room` упрощает компенсацию и не требует изменения состояния комнаты.

### ADR-4: Децентрализованная валидация JWT

**Контекст.** Gateway не должен быть единой точкой отказа для авторизации.

**Решение.** Каждый сервис валидирует JWT самостоятельно. Hotel Service использует стандартный OAuth2 Resource Server, а Booking Service реализует кастомный `JwtAuthenticationFilter` для гибкой интеграции с внутренней логикой.

**Обоснование.** Децентрализованная проверка повышает отказоустойчивость и позволяет сервисам работать независимо от Gateway.

### ADR-5: Resilience4j для межсервисных вызовов

**Контекст.** Hotel Service может быть временно недоступен.

**Решение.** Retry (3 попытки, экспоненциальный backoff) + CircuitBreaker (sliding window 20, threshold 50%).

**Обоснование.** Защита от каскадных отказов и автоматическое восстановление после сбоев. Исключения `CallNotPermittedException` явно перехватываются и преобразуются в 503 Service Unavailable.

### ADR-6: Сквозная трассировка через `X-Request-Id`

**Контекст.** Распределённый запрос проходит через несколько сервисов.

**Решение.** Gateway генерирует/пропагирует `X-Request-Id` (с санитизацией), каждый сервис кладёт его в MDC и логирует.

**Обоснование.** Единый идентификатор позволяет коррелировать логи всех сервисов для одного запроса.

### ADR-7: Реконсиляция и TTL блокировок

**Контекст.** При сбоях сети бронирование может остаться в статусе `PENDING` или `CANCELLING`, а блокировка номера - в статусе `HELD` без подтверждения.

**Решение.**

1. Блокировка `RoomLock` имеет статусы `HELD` (с TTL 5 минут) и `CONFIRMED` (без TTL).
2. Счётчик `times_booked` увеличивается только при успешном вызове `/commit`, а не при `/confirm-availability`.
3. Фоновый джоб `ReconciliationService` удаляет только просроченные `HELD` блокировки, не затрагивая подтверждённые.
4. `BookingService` периодически проверяет зависшие `PENDING` и `CANCELLING` брони и инициирует компенсацию (`release`), при этом удалённые вызовы выполняются вне транзакций БД во избежание длительных блокировок.

**Обоснование.** Разделение состояний предотвращает деградацию статистики и повторное бронирование подтверждённых номеров, а реконсиляция гарантирует eventual consistency без распределённых транзакций.
