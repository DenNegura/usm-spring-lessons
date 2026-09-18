package com.example.orders.notification;

import com.example.orders.domain.Order;

public interface NotificationSender {
    void send(Order order, String message);
}