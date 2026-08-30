package com.example.orders.spring;

public interface NotificationSender {
    void send(Order order, String message);
}

// NotificationSender говорит: «объект умеет отправить сообщение».