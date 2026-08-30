package com.example.orders.spring;

import org.springframework.stereotype.Component;

@Component("smsSender")
public class SmsNotificationSender implements NotificationSender {
    public void send(Order order, String message) {
        System.out.println("SMS -> " + order.phone() + ": " + message);
    }
}
