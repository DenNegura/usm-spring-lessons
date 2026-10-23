package com.example.orders.demo;

import com.example.orders.entitie.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataSupport {

    public static final long FIRST = 6001;

    @PersistenceContext
    private EntityManager em;

    public static OrderEntity newOrder(long id) {
        return new OrderEntity(
            id,
            "data-demo@example.com",
            "+37360000000",
            new BigDecimal("100.00"),
            new BigDecimal("100.00")
        );
    }

    // Только зарезервированные id этого урока; подготовка коммитится до опыта.
    @Transactional
    public void reset() {
        System.out.println("=== PREPARE (separate transaction) ===");
        em.createQuery(
            "delete from OrderItemEntity i where i.order.id between 6001 and 6006"
        ).executeUpdate();
        em.createQuery(
            "delete from OrderEntity o where o.id between 6001 and 6006"
        ).executeUpdate();
        em.createQuery(
            "delete from ProductEntity p where p.id in (6101, 6102)"
        ).executeUpdate();
        em.createQuery(
            "delete from CustomerEntity c where c.id in (6201, 6202)"
        ).executeUpdate();
        CustomerEntity customer = new CustomerEntity(6201L, "Anna");
        em.persist(customer);
        em.persist(new CustomerEntity(6202L, "Boris"));
        ProductEntity product = new ProductEntity(6101L, "Book");
        em.persist(product);
        for (long id = 6001; id <= 6005; id++) {
            OrderEntity order = newOrder(id);
            order.changeCustomer(customer);
            order.addItem(new OrderItemEntity(6300 + id - 6000, product, 1));
            em.persist(order);
        }
    }
}
