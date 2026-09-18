package com.example.orders.repository;

import com.example.orders.domain.Order;

import java.util.Optional;

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(long id);
}

// OrderRepository говорит: «объект умеет сохранить заказ»
