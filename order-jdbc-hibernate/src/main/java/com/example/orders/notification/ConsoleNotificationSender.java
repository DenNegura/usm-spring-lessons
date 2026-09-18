package com.example.orders.notification;

import com.example.orders.domain.Order;

public class ConsoleNotificationSender implements NotificationSender {
    public void send(Order order, String message) {
        System.out.println("CONSOLE -> " + order.id() + ": " + message);
    }
}