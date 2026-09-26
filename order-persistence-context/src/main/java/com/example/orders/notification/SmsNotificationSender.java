package com.example.orders.notification;

import com.example.orders.domain.Order;

public class SmsNotificationSender implements NotificationSender {
    public void send(Order order, String message) {
        System.out.println("SMS -> " + order.phone() + ": " + message);
    }
}
