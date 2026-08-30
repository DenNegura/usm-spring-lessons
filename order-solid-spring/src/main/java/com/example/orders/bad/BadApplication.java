package com.example.orders.bad;

import java.math.BigDecimal;

public class BadApplication {
    public static void main(String[] args) {
        OrderService service = new OrderService();
        Order order = service.placeOrder(
                101L,
                "student@example.com",
                "+79990000000",
                new BigDecimal("1500.00"),
                "EMAIL"
        );
        System.out.println("Created: " + order);
    }
}
