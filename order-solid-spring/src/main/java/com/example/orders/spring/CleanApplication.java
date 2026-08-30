package com.example.orders.spring;

import java.math.BigDecimal;

public class CleanApplication {
    public static void main(String[] args) {
        // Здесь мы создаём объекты и соединяем их между собой.
        OrderValidator validator = new OrderValidator();
        OrderRepository repository = new ConsoleOrderRepository();
        NotificationSender sender = new EmailNotificationSender();

        OrderService service =
                new OrderService(validator, repository, sender);

        service.placeOrder(
                102L,
                "student@example.com",
                "+79990000000",
                new BigDecimal("1500.00")
        );
    }
}

// OrderService получает validator, repository и sender через конструктор.
// По-английски это называется constructor injection.
// Это один из вариантов Dependency Injection, или DI.
// Объект не создаёт помощников сам — их передаёт код снаружи.