package com.example.orders.spring;

public interface OrderRepository {
    void save(Order order);
}

// OrderRepository говорит: «объект умеет сохранить заказ»
