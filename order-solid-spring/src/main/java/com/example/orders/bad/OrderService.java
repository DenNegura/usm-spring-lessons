package com.example.orders.bad;

import java.math.BigDecimal;

public class OrderService {
    public Order placeOrder(long id, String email, String phone,
                            BigDecimal subtotal, String channel) {
        // [Проблема 1] ...
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (subtotal == null || subtotal.signum() <= 0) {
            throw new IllegalArgumentException("Subtotal must be positive");
        }

        // Бизнес-правило: крупный заказ получает скидку 10%.
        BigDecimal total = subtotal.compareTo(new BigDecimal("1000")) >= 0
                ? subtotal.multiply(new BigDecimal("0.90"))
                : subtotal;
        Order order = new Order(id, email, phone, subtotal, total);

        // [Проблема 2] ...
        // [Проблема 3] ...
        ConsoleOrderRepository repository = new ConsoleOrderRepository();
        repository.save(order);

        String message = "Order " + id + " created. Total: " + total;

        // [Проблема 4] ...
        // [Проблема 5] ...
        if ("EMAIL".equalsIgnoreCase(channel)) {
            new EmailNotificationSender().send(email, message);
        } else if ("SMS".equalsIgnoreCase(channel)) {
            new SmsNotificationSender().send(phone, message);
        } else {
            throw new IllegalArgumentException("Unknown channel: " + channel);
        }
        return order;
    }
}
