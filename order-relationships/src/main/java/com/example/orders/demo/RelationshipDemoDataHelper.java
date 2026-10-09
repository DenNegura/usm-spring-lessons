package com.example.orders.demo;

import com.example.orders.entitie.CustomerEntity;
import com.example.orders.entitie.OrderEntity;
import com.example.orders.entitie.OrderItemEntity;
import com.example.orders.entitie.ProductEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RelationshipDemoDataHelper {
    public static final long CUSTOMER_A = 501, CUSTOMER_B = 502;
    public static final long ORDER_ID = 601, PRODUCT_ID = 701;
    public static final long ITEM_A = 801, ITEM_B = 802;
    public static final long PROFILE_ID = 901, STUDENT_ID = 1001, COURSE_ID = 1101;
    private final EntityManagerFactory factory;

    public RelationshipDemoDataHelper(EntityManagerFactory factory) {
        this.factory = factory;
    }

    public static OrderEntity newOrder(long id) {
        BigDecimal total = new BigDecimal("100.00");
        return new OrderEntity(id, "relationships-demo@example.com", "+37360000000", total, total);
    }

    // Только зарезервированные Lesson 5 id. Подготовка — отдельная transaction.
    // Native SQL делает fixture явной и отделяет её от демонстрируемого cascade.
    public void prepare() {
        RelationshipDemoRunner.section("PREPARE: reserved Lesson 5 rows");
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                em.createNativeQuery("DELETE FROM student_courses WHERE student_id = 1001").executeUpdate();
                em.createNativeQuery("DELETE FROM customer_profiles WHERE id = 901").executeUpdate();
                em.createNativeQuery("DELETE FROM order_items WHERE order_id = 601").executeUpdate();
                em.createNativeQuery("DELETE FROM orders WHERE id = 601").executeUpdate();
                em.createNativeQuery("""
                        INSERT INTO customers(id, name, address_city, address_street)
                        VALUES (501, 'Customer A', NULL, NULL), (502, 'Customer B', NULL, NULL)
                        ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name,
                            address_city = NULL, address_street = NULL
                        """).executeUpdate();
                em.createNativeQuery("""
                        INSERT INTO products(id, name, price) VALUES (701, 'Notebook', 100.00)
                        ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, price = EXCLUDED.price
                        """).executeUpdate();
                em.createNativeQuery("INSERT INTO students(id, name) VALUES (1001, 'Student A') ON CONFLICT (id) DO NOTHING").executeUpdate();
                em.createNativeQuery("INSERT INTO courses(id, name) VALUES (1101, 'JPA') ON CONFLICT (id) DO NOTHING").executeUpdate();
                tx.commit();
            } catch (RuntimeException | Error failure) {
                if (tx.isActive()) tx.rollback();
                throw failure;
            }
        }
    }

    public void prepareOrder(boolean withItems) {
        prepare();
        RelationshipDemoRunner.section("PREPARE: existing order" + (withItems ? " with two items" : ""));
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                OrderEntity order = newOrder(ORDER_ID);
                em.find(CustomerEntity.class, CUSTOMER_A).addOrder(order);
                if (withItems) {
                    ProductEntity product = em.find(ProductEntity.class, PRODUCT_ID);
                    order.addItem(new OrderItemEntity(ITEM_A, product, 1));
                    order.addItem(new OrderItemEntity(ITEM_B, product, 2));
                }
                em.persist(order);
                tx.commit();
            } catch (RuntimeException | Error failure) {
                if (tx.isActive()) tx.rollback();
                throw failure;
            }
        }
    }

    public void verifyCustomer(long expectedId) {
        RelationshipDemoRunner.section("VERIFY DATABASE: new EntityManager");
        try (EntityManager em = factory.createEntityManager()) {
            long actual = ((Number) em.createNativeQuery("SELECT customer_id FROM orders WHERE id = 601")
                    .getSingleResult()).longValue();
            System.out.println("orders.customer_id = " + actual);
            RelationshipDemoRunner.check(actual == expectedId, "FK follows OrderEntity.customer");
            OrderEntity order = em.find(OrderEntity.class, ORDER_ID);
            RelationshipDemoRunner.check(order.getCustomer().getId() == expectedId, "Java reference restored from FK");
        }
    }

    public void verifyItems(long expectedCount) {
        RelationshipDemoRunner.section("VERIFY DATABASE: new EntityManager");
        try (EntityManager em = factory.createEntityManager()) {
            long count = ((Number) em.createNativeQuery("SELECT count(*) FROM order_items WHERE order_id = 601")
                    .getSingleResult()).longValue();
            System.out.println("order_items count = " + count);
            RelationshipDemoRunner.check(count == expectedCount, "expected child count");
            RelationshipDemoRunner.check(em.find(ProductEntity.class, PRODUCT_ID) != null, "independent Product still exists");
        }
    }
}
