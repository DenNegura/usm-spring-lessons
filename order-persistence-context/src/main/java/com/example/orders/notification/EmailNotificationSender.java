package com.example.orders.notification;

import com.example.orders.domain.Order;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationSender implements NotificationSender {
    public void send(Order order, String message) {
        System.out.println("EMAIL -> " + order.email() + ": " + message);
    }
}