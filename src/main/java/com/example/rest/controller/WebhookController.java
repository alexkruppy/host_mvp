package com.example.rest.controller;

import com.example.rest.service.StripeService;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    private final StripeService stripeService;

    public WebhookController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        if (webhookSecret != null && !webhookSecret.isEmpty()) {
            try {
                Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
                handleEvent(event);
            } catch (Exception e) {
                log.error("Webhook signature verification failed: {}", e.getMessage());
                return ResponseEntity.badRequest().body("Signature verification failed");
            }
        } else {
            log.warn("Stripe webhook secret not configured, processing raw event");
        }

        return ResponseEntity.ok("OK");
    }

    private void handleEvent(Event event) {
        switch (event.getType()) {
            case "checkout.session.completed" -> {
                Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                if (session != null) {
                    stripeService.handleCheckoutCompleted(session.getId());
                }
            }
            case "customer.subscription.deleted" -> {
                log.info("Subscription deleted event received");
            }
            default -> log.debug("Unhandled event type: {}", event.getType());
        }
    }
}
