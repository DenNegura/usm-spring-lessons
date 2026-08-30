package com.example.orders.spring;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.math.BigDecimal;

public class SpringApplication {

    public static void main(String[] args) {
        var context = new AnnotationConfigApplicationContext(ApplicationConfig.class);

        OrderService orderService = context.getBean(OrderService.class);

        orderService.placeOrder(
                103L,
                "student@example.com",
                "+79990000000",
                new BigDecimal("1500.00")
        );
    }
}
