package com.example.orders.boot.demo;

import com.example.orders.boot.service.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class OrderDemoRunner implements CommandLineRunner {

    private final OrderService orderService;

    private final DemoProperties properties;

    private final String profile;

    public OrderDemoRunner(OrderService orderService, DemoProperties properties, @Value("${spring.profiles.active:default}") String profile) {
        this.orderService = orderService;
        this.properties = properties;
        this.profile = profile;
    }

    @Override
    public void run(String... args) {
        System.out.println("Order Demo Runner is starting with profile: " + this.profile);
        if (!properties.enabled()) {
            System.out.println("DEMO DISABLED");
            return;
        }

        orderService.placeOrder(
                properties.id(),
                properties.email(),
                properties.phone(),
                properties.subtotal()
        );

    }
}
