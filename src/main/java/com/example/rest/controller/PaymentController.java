package com.example.rest.controller;

import com.example.rest.dto.CheckoutResponse;
import com.example.rest.model.User;
import com.example.rest.repository.UserRepository;
import com.example.rest.service.StripeService;
import com.stripe.exception.StripeException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final StripeService stripeService;
    private final UserRepository userRepository;

    public PaymentController(StripeService stripeService, UserRepository userRepository) {
        this.stripeService = stripeService;
        this.userRepository = userRepository;
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            CheckoutResponse response = stripeService.createCheckoutSession(user);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getSubscriptionStatus(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(Map.of(
                "role", user.getRole().name(),
                "premiumActive", user.isPremiumActive(),
                "premiumExpiry", user.getPremiumExpiry() != null ? user.getPremiumExpiry().toString() : null
        ));
    }
}
