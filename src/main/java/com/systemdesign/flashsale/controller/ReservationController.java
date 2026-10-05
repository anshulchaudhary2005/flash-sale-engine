package com.systemdesign.flashsale.controller;

import com.systemdesign.flashsale.service.StockReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final StockReservationService stockReservationService;

    public ReservationController(StockReservationService stockReservationService) {
        this.stockReservationService = stockReservationService;
    }

    @PostMapping
    public ResponseEntity<String> reserveStock(@RequestBody ReservationRequest request) {
        try {
            boolean success = stockReservationService.reserve(
                    request.getItemId(),
                    request.getUserId(),
                    request.getQuantity()
            );

            if (success) {
                return ResponseEntity.ok("Reservation successful for user: " + request.getUserId());
            } else {
                return ResponseEntity.badRequest().body("Reservation failed: Sold out or insufficient stock.");
            }
        } catch (IllegalStateException e) {
            // Catches "User already has an active reservation"
            return ResponseEntity.badRequest().body("Reservation failed: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // Catches "Item stock not initialized in Redis"
            return ResponseEntity.badRequest().body("System Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("An unexpected error occurred.");
        }
    }
}