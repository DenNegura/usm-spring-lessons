package com.example.orders.clean;

public class SmsNotificationSender implements NotificationSender {
    public void send(Order order, String message) {
        System.out.println("SMS -> " + order.phone() + ": " + message);
    }
}
