package com.systemdesign.flashsale.service;

import org.springframework.data.redis.core.StringRedisTemplate; // Use this
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockReservationService {

    private final StringRedisTemplate stringRedisTemplate; // Change here
    private final RedisScript<Long> reserveStockScript;

    public StockReservationService(StringRedisTemplate stringRedisTemplate, RedisScript<Long> reserveStockScript) { // Change here
        this.stringRedisTemplate = stringRedisTemplate;
        this.reserveStockScript = reserveStockScript;
    }

    public boolean reserve(String itemId, String userId, int quantity) {
        String stockKey = "item:" + itemId + ":stock";
        String purchasedUsersSetKey = "item:" + itemId + ":purchased_users";
        String syncQueueListKey = "item:" + itemId + ":sync_queue"; // Add the third key

        try {
            Long result = stringRedisTemplate.execute(
                    reserveStockScript,
                    List.of(stockKey, purchasedUsersSetKey, syncQueueListKey), // Pass 3 keys here
                    userId,
                    String.valueOf(quantity)
            );

            if (result == null) return false;
            if (result == -1L) throw new IllegalArgumentException("Item stock not initialized in Redis.");
            if (result == -2L) throw new IllegalStateException("User already has an active reservation.");

            return result == 1L;

        } catch (Exception e) {
            System.err.println("Redis Script Execution Failed: " + e.getMessage());
            return false;
        }
    }
}