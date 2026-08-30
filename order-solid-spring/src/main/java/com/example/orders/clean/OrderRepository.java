package com.example.orders.clean;

public interface OrderRepository {
    void save(Order order);
}

// OrderRepository говорит: «объект умеет сохранить заказ»
