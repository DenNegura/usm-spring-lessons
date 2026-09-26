package com.example.orders.service;

import com.example.orders.notification.NotificationSender;
import com.example.orders.domain.Order;
import com.example.orders.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class OrderService {
    private final OrderValidator validator;
    private final OrderRepository repository;
    private final NotificationSender notificationSender;

    public OrderService(OrderValidator validator,
                        OrderRepository repository,
                        NotificationSender notificationSender) {
        this.validator = validator;
        this.repository = repository;
        this.notificationSender = notificationSender;
    }

    @Transactional
    public Order placeOrder(long id, String email, String phone,
                            BigDecimal subtotal) {
        validator.validate(email, subtotal);

        BigDecimal total = subtotal.compareTo(new BigDecimal("1000")) >= 0
                ? subtotal.multiply(new BigDecimal("0.90"))
                : subtotal;
        Order order = new Order(id, email, phone, subtotal, total);

        repository.save(order);
        notificationSender.send(
                order,
                "Order " + id + " created. Total: " + total
        );
        return order;
    }


    public Order getOrderById(long id) {
        return repository.findById(id).orElse(null);
    }
}