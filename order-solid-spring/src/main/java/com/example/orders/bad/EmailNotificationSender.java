package com.example.orders.bad;

public class EmailNotificationSender {
    public void send(String email, String message) {
        System.out.println("EMAIL -> " + email + ": " + message);
    }
}