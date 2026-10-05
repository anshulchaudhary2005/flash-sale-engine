# High-Concurrency Flash Sale Engine

A proof-of-concept backend system designed to handle massive concurrent traffic spikes without overselling inventory. This project demonstrates how to solve the "Thundering Herd" problem and strict inventory management using Spring Boot, Redis, and PostgreSQL.

This is Project 1 of my 10-project distributed systems backend engineering roadmap.

## System Architecture

The architecture separates high-speed validation from durable persistence to achieve both speed and data integrity:

1. **Atomic Validation (Redis & Lua):**
   Incoming HTTP requests are immediately routed to Redis. A custom Lua script executes atomically to check inventory and verify the user hasn't already purchased the item (using a Redis `SET`). This completely bypasses traditional relational database row-level locking bottlenecks.
2. **Asynchronous Queueing:**
   Successful reservations are pushed to a Redis `LIST` (`sync_queue`), acting as an in-memory buffer.
3. **Durable Persistence (Spring Boot Scheduler):**
   A background `@Scheduled` worker wakes up periodically to drain the Redis queue and batch-insert the reservations into PostgreSQL for permanent storage.
4. **Distributed Locking:**
   The background sync worker uses a distributed lock (`setIfAbsent`) to ensure thread safety. If multiple instances of this application are running, only one instance will acquire the lock and process the queue, preventing duplicate database entries.

## Tech Stack
* **Language:** Java 17
* **Framework:** Spring Boot (Web, Data JPA)
* **High-Speed Cache/Queue:** Redis (via Docker)
* **Relational Database:** PostgreSQL (via Docker)
* **Load Testing:** Apache JMeter

## How to Run Locally

1. Start the infrastructure using Docker Compose:
   ```bash
   docker-compose up -d