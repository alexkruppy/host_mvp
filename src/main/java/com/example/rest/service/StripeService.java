package com.example.rest.service;

import com.example.rest.dto.CheckoutResponse;
import com.example.rest.model.Payment;
import com.example.rest.model.Role;
import com.example.rest.model.User;
import com.example.rest.repository.PaymentRepository;
import com.example.rest.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.price-id:price_monthly_premium}")
    private String priceId;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public StripeService(PaymentRepository paymentRepository, UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    public CheckoutResponse createCheckoutSession(User user) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(baseUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(baseUrl + "/payment/cancel")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(priceId)
                                .setQuantity(1L)
                                .build()
                )
                .setCustomerEmail(user.getEmail())
                .putMetadata("user_id", user.getId().toString())
                .build();

        Session session = Session.create(params);

        Payment payment = new Payment(user, session.getId(), 0L, "usd");
        payment.setStatus("pending");
        paymentRepository.save(payment);

        return new CheckoutResponse(session.getId(), session.getUrl());
    }

    public void handleCheckoutCompleted(String sessionId) {
        Optional<Payment> optPayment = paymentRepository.findByStripeSessionId(sessionId);
        if (optPayment.isEmpty()) {
            log.warn("Payment not found for session: {}", sessionId);
            return;
        }

        Payment payment = optPayment.get();
        payment.setStatus("completed");
        paymentRepository.save(payment);

        User user = payment.getUser();
        user.setRole(Role.PREMIUM);
        user.setPremiumExpiry(LocalDateTime.now().plusMonths(1));
        userRepository.save(user);

        log.info("User {} upgraded to PREMIUM until {}", user.getEmail(), user.getPremiumExpiry());
    }

    public void handleSubscriptionDeleted(String subscriptionId) {
        log.info("Subscription deleted: {}", subscriptionId);
    }
}
