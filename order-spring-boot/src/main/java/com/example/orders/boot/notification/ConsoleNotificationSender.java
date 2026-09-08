package com.example.orders.boot.notification;

import com.example.orders.boot.domain.Order;

public class ConsoleNotificationSender implements NotificationSender {
    public void send(Order order, String message) {
        System.out.println("CONSOLE -> " + order.id() + ": " + message);
    }
}