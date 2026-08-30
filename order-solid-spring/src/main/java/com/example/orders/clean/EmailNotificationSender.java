package com.example.orders.clean;

public class EmailNotificationSender implements NotificationSender {
    public void send(Order order, String message) {
        System.out.println("EMAIL -> " + order.email() + ": " + message);
    }
}