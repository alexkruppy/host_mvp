package com.example.rest.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/auth/login")
    public String login() {
        return "login";
    }

    @GetMapping("/auth/register")
    public String register() {
        return "register";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/upload")
    public String upload() {
        return "upload";
    }

    @GetMapping("/premium")
    public String premium() {
        return "premium";
    }

    @GetMapping("/watch/{id}")
    public String watch() {
        return "watch";
    }

    @GetMapping("/payment/success")
    public String paymentSuccess() {
        return "payment-success";
    }

    @GetMapping("/payment/cancel")
    public String paymentCancel() {
        return "payment-cancel";
    }
}
