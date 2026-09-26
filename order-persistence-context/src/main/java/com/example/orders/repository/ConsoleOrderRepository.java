package com.example.orders.repository;

import com.example.orders.domain.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Profile("console")
public class ConsoleOrderRepository implements OrderRepository {
    public void save(Order order) {
        System.out.println(
                "ORDER SAVED -> id=" + order.id()
                        + ", total=" + order.total()
        );
    }

    @Override
    public Optional<Order> findById(long id) {
        return Optional.empty();
    }
}

// Слово implements означает: класс обязуется предоставить методы интерфейса.
// Поэтому ConsoleOrderRepository обязан содержать метод save(Order order).