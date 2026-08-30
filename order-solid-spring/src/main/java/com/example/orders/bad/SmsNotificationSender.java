package com.example.orders.bad;

public class SmsNotificationSender {
    public void send(String phone, String message) {
        System.out.println("SMS -> " + phone + ": " + message);
    }
}
