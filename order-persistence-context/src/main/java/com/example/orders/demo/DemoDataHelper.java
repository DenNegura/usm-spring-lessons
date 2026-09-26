package com.example.orders.demo;

import com.example.orders.entitie.OrderEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class DemoDataHelper {
    public static final BigDecimal INITIAL_TOTAL = new BigDecimal("100.00");
    private final EntityManagerFactory factory;

    public DemoDataHelper(EntityManagerFactory factory) {
        this.factory = factory;
    }

    public static OrderEntity newOrder(long id) {
        return new OrderEntity(id, "persistence-demo@example.com", "+37360000000",
                INITIAL_TOTAL, INITIAL_TOTAL);
    }

    // Только зарезервированные учебные id; подготовка коммитится отдельно.
    public void reset(long id) {
        System.out.println("=== PREPARE demo row " + id + " ===");
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                OrderEntity order = em.find(OrderEntity.class, id);
                if (order == null) em.persist(newOrder(id));
                else order.changeTotal(INITIAL_TOTAL);
                transaction.commit();
            } catch (RuntimeException | Error failure) {
                if (transaction.isActive()) transaction.rollback();
                throw failure;
            }
        }
    }

    public void clearBatchRows() {
        System.out.println("=== PREPARE batch rows 2201..2223 ===");
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                for (long id = 2201; id <= 2223; id++) {
                    OrderEntity order = em.find(OrderEntity.class, id);
                    if (order != null) em.remove(order);
                }
                transaction.commit();
            } catch (RuntimeException | Error failure) {
                if (transaction.isActive()) transaction.rollback();
                throw failure;
            }
        }
    }

    public void verify(long id, BigDecimal expected) {
        System.out.println("=== VERIFY FROM NEW CONTEXT ===");
        // Новый context исключает чтение ранее загруженного Java object из L1 cache.
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                OrderEntity order = em.find(OrderEntity.class, id);
                System.out.println("DB total: " + (order == null ? "<absent>" : order.getTotal()));
                if (expected == null ? order != null : order == null || expected.compareTo(order.getTotal()) != 0) {
                    throw new IllegalStateException("Unexpected DB state for id=" + id);
                }
                transaction.commit();
            } catch (RuntimeException | Error failure) {
                if (transaction.isActive()) transaction.rollback();
                throw failure;
            }
        }
    }

    public void verifyBatch() {
        System.out.println("=== VERIFY ALL 23 ROWS FROM NEW CONTEXT ===");
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                for (long id = 2201; id <= 2223; id++) {
                    OrderEntity order = em.find(OrderEntity.class, id);
                    if (order == null || order.getTotal().compareTo(INITIAL_TOTAL) != 0) {
                        throw new IllegalStateException("Missing/incorrect batch row " + id);
                    }
                }
                transaction.commit();
                System.out.println("Verified 23 committed rows");
            } catch (RuntimeException | Error failure) {
                if (transaction.isActive()) transaction.rollback();
                throw failure;
            }
        }
    }
}
