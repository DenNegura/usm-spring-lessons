package com.example.orders.boot.repository;

import com.example.orders.boot.domain.Order;

public interface OrderRepository {
    void save(Order order);
}

// OrderRepository говорит: «объект умеет сохранить заказ»
