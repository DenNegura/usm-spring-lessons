package com.example.orders.projection;

import java.math.BigDecimal;

public record OrderSummary(Long id, String customerName, BigDecimal total) {}
