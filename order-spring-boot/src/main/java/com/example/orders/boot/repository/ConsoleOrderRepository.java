package com.example.orders.boot.repository;

import com.example.orders.boot.domain.Order;
import org.springframework.stereotype.Repository;

@Repository
public class ConsoleOrderRepository implements OrderRepository {
    public void save(Order order) {
        System.out.println(
                "ORDER SAVED -> id=" + order.id()
                        + ", total=" + order.total()
        );
    }
}

// Слово implements означает: класс обязуется предоставить методы интерфейса.
// Поэтому ConsoleOrderRepository обязан содержать метод save(Order order).