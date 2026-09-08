package com.example.orders.boot.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.demo")
public record DemoProperties(
        boolean enabled,
        long id,
        String email,
        String phone,
        BigDecimal subtotal
) {}

