package com.systemdesign.flashsale.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
class StockReservationServiceTest {

    // Testcontainers will automatically spin up a temporary Postgres database for this test
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("test_db")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private StockReservationService stockReservationService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;
    private final String ITEM_ID = "101";

    @BeforeEach
    void setup() {
        // Delete the specific keys used in this test instead of flushing the whole DB
        stringRedisTemplate.delete("item:" + ITEM_ID + ":stock");
        stringRedisTemplate.delete("item:" + ITEM_ID + ":reservations");

        // Initialize stock to exactly 10 items
        stringRedisTemplate.opsForValue().set("item:" + ITEM_ID + ":stock", "10");
    }

    @Test
    void testConcurrentReservations_NoOverselling() throws InterruptedException {
        // --- DEBUG LINES START ---
        String currentStockInRedis = stringRedisTemplate.opsForValue().get("item:" + ITEM_ID + ":stock");
        System.out.println("====== DEBUG: Redis stock value BEFORE test: '" + currentStockInRedis + "' ======");


        // --- DEBUG LINES END ---
        int numberOfThreads = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1); // Used to start all threads at the exact same time
        CountDownLatch completionLatch = new CountDownLatch(numberOfThreads); // Waits for all threads to finish

        AtomicInteger successfulReservations = new AtomicInteger(0);
        AtomicInteger failedReservations = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final String userId = "user_" + i;
            executorService.submit(() -> {
                try {
                    latch.await(); // All threads wait here until the latch is released

                    boolean success = stockReservationService.reserve(ITEM_ID, userId, 1);
                    if (success) {
                        successfulReservations.incrementAndGet();
                    } else {
                        failedReservations.incrementAndGet();
                    }
                } catch (Exception e) {
                    failedReservations.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Release the hounds! All 50 threads execute reserve() concurrently
        latch.countDown();

        completionLatch.await();

        assertEquals(10, successfulReservations.get(), "Exactly 10 reservations should succeed");
        assertEquals(40, failedReservations.get(), "Exactly 40 reservations should fail due to stock depletion");

        // Verify final stock is exactly 0
        Object rawStock = stringRedisTemplate.opsForValue().get("item:" + ITEM_ID + ":stock");
        String remainingStock = rawStock != null ? String.valueOf(rawStock) : "null";
        assertEquals("0", remainingStock, "Stock should be exactly zero");
    }
}