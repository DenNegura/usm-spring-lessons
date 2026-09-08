package com.example.orders.boot.notification;

import com.example.orders.boot.domain.Order;
import org.springframework.stereotype.Component;

@Component("emailSender")
public class EmailNotificationSender implements NotificationSender {
    public void send(Order order, String message) {
        System.out.println("EMAIL -> " + order.email() + ": " + message);
    }
}