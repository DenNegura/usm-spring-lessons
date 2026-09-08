package com.example.orders.boot.demo;

import com.example.orders.boot.service.OrderService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderDemoRunner implements CommandLineRunner {
    private final OrderService orderService;

    public OrderDemoRunner(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void run(String... args) {
        orderService.placeOrder(
                201L,
                "student@example.com",
                "+37360000000",
                new BigDecimal("1500.00")
        );
    }
}
