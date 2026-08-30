package com.example.orders.clean;

import java.math.BigDecimal;

public class OrderValidator {

    public void validate(String email, BigDecimal subtotal) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (subtotal == null || subtotal.signum() <= 0) {
            throw new IllegalArgumentException("Subtotal must be positive");
        }
    }
}
