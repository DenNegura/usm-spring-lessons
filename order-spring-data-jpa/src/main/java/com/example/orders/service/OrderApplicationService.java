package com.example.orders.service;

import com.example.orders.demo.DemoDataSupport;
import com.example.orders.repository.*;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderApplicationService {
    private final OrderJpaRepository orders;
    private final CustomerJpaRepository customers;

    public OrderApplicationService(OrderJpaRepository orders, CustomerJpaRepository customers) {
        this.orders = orders; this.customers = customers;
    }

    @Transactional
    public void changeCustomerAndCreateOrder(boolean fail) {
        System.out.println("=== TRANSACTION START ===");
        var customer = customers.findById(6202L).orElseThrow();
        var order = orders.findById(6001L).orElseThrow();
        System.out.println("=== CHANGE DATA ===");
        order.changeCustomer(customer);
        order.changeTotal(new BigDecimal("250.00"));
        var second = DemoDataSupport.newOrder(6006L);
        second.changeCustomer(customer);
        orders.save(second);
        // Делаем SQL видимым даже в опыте rollback: flush ещё не commit.
        orders.flush();
        if (fail) {
            System.out.println("=== THROW EXCEPTION ===");
            throw new DemoRollbackException();
        }
    }

    public static class DemoRollbackException extends RuntimeException {}
}
