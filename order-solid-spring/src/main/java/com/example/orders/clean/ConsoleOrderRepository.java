package com.example.orders.clean;

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