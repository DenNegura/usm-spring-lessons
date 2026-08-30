package com.example.orders.clean;

public interface NotificationSender {
    void send(Order order, String message);
}

// NotificationSender говорит: «объект умеет отправить сообщение».