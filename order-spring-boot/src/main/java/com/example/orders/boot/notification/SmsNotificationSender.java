package com.example.orders.boot.notification;

import com.example.orders.boot.domain.Order;

public class SmsNotificationSender implements NotificationSender {
    public void send(Order order, String message) {
        System.out.println("SMS -> " + order.phone() + ": " + message);
    }
}
