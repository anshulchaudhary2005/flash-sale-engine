package com.systemdesign.flashsale.repository.sync;

import com.systemdesign.flashsale.repository.ReservationEntity;
import com.systemdesign.flashsale.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RedisToPostgresSyncService {

    private static final Logger log = LoggerFactory.getLogger(RedisToPostgresSyncService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ReservationRepository reservationRepository;

    private final String ITEM_ID = "101"; // Hardcoded for this milestone
    private final String SYNC_LOCK_KEY = "lock:sync:item:" + ITEM_ID;
    private final String RESERVATIONS_KEY = "item:" + ITEM_ID + ":reservations";
    private final String SYNC_QUEUE_KEY = "item:" + ITEM_ID + ":sync_queue";
    public RedisToPostgresSyncService(StringRedisTemplate stringRedisTemplate, ReservationRepository reservationRepository) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.reservationRepository = reservationRepository;
    }

    // Runs every 10 seconds
    @Scheduled(fixedRate = 10000)
    public void syncReservations() {
        System.out.println("====== SCHEDULER WOKE UP ======");
        // 1. Attempt to acquire the distributed lock
        // The lock automatically expires after 5 seconds to prevent deadlocks if the server crashes
        Boolean lockAcquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(SYNC_LOCK_KEY, "locked", Duration.ofSeconds(5));

        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                processSync();
            } finally {
                // Always release the lock when done
                stringRedisTemplate.delete(SYNC_LOCK_KEY);
            }
        } else {
            log.debug("Another instance is currently syncing. Skipping this cycle.");
        }
    }

    @Transactional
    protected void processSync() {
        System.out.println("--- SYNC PROCESS STARTED ---"); // DEBUG LOG

        List<String> queuedItems = stringRedisTemplate.opsForList().range(SYNC_QUEUE_KEY, 0, 99);

        if (queuedItems == null || queuedItems.isEmpty()) {
            System.out.println("Sync queue is empty. Exiting."); // DEBUG LOG
            return;
        }

        System.out.println("Found " + queuedItems.size() + " items in the queue."); // DEBUG LOG
        List<ReservationEntity> entitiesToSave = new ArrayList<>();

        for (String itemData : queuedItems) {
            System.out.println("Processing item: " + itemData); // DEBUG LOG
            try {
                String[] parts = itemData.split(":");
                String userId = parts[0];
                int quantity = Integer.parseInt(parts[1]);

                entitiesToSave.add(new ReservationEntity(userId, ITEM_ID, quantity));
            } catch (Exception e) {
                System.err.println("Error parsing item data: " + itemData + " - " + e.getMessage()); // DEBUG LOG
            }
        }

        System.out.println("Attempting to save " + entitiesToSave.size() + " entities to Postgres..."); // DEBUG LOG

        try {
            reservationRepository.saveAll(entitiesToSave);
            System.out.println("Save successful!"); // DEBUG LOG

            stringRedisTemplate.opsForList().trim(SYNC_QUEUE_KEY, queuedItems.size(), -1);
            System.out.println("Trimmed Redis queue."); // DEBUG LOG

        } catch (Exception e) {
            System.err.println("CRITICAL DB SAVE ERROR: " + e.getMessage()); // DEBUG LOG
            e.printStackTrace(); // Print full stack trace
        }
        System.out.println("--- SYNC PROCESS FINISHED ---"); // DEBUG LOG
    }
}