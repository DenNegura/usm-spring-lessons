package com.example.orders.boot.notification;

import com.example.orders.boot.domain.Order;

public interface NotificationSender {
    void send(Order order, String message);
}