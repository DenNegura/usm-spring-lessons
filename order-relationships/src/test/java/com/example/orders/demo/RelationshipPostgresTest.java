package com.example.orders.demo;

import com.example.orders.OrderApplication;
import com.example.orders.entitie.CustomerEntity;
import com.example.orders.entitie.CustomerProfileEntity;
import com.example.orders.entitie.OrderEntity;
import com.example.orders.entitie.OrderItemEntity;
import com.example.orders.entitie.ProductEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static com.example.orders.demo.RelationshipDemoDataHelper.CUSTOMER_A;
import static com.example.orders.demo.RelationshipDemoDataHelper.ORDER_ID;
import static com.example.orders.demo.RelationshipDemoDataHelper.PRODUCT_ID;
import static com.example.orders.demo.RelationshipDemoDataHelper.ITEM_A;
import static com.example.orders.demo.RelationshipDemoDataHelper.ITEM_B;

// Явно включается для существующего PostgreSQL с вручную применённой schema.
@EnabledIfSystemProperty(named = "relationships.integration", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RelationshipPostgresTest {
    private ConfigurableApplicationContext context;
    private RelationshipDemoRunner runner;
    private EntityManagerFactory factory;

    @BeforeAll
    void start() {
        context = SpringApplication.run(OrderApplication.class,
                "--app.demo.relationship-experiment=");
        runner = context.getBean(RelationshipDemoRunner.class);
        factory = context.getBean(EntityManagerFactory.class);
        try (EntityManager em = factory.createEntityManager()) {
            assertEquals("lesson5", em.createNativeQuery("SELECT current_schema()").getSingleResult());
        }
    }

    @AfterAll
    void close() { if (context != null) context.close(); }

    @Test void manyToOne() { runner.experimentManyToOne(); }
    @Test void owningSide() { runner.experimentOwningSide(); }
    @Test void cascadePersist() { runner.experimentCascadePersist(); }
    @Test void orphanRemoval() { runner.experimentOrphanRemoval(); }
    @Test void oneToOne() { runner.experimentOneToOne(); }
    @Test void manyToMany() { runner.experimentManyToMany(); }
    @Test void embeddable() { runner.experimentEmbeddable(); }

    @Test
    void removingOrderCascadesToItemsButNeverToProduct() {
        context.getBean(RelationshipDemoDataHelper.class).prepareOrder(true);
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                em.remove(em.find(OrderEntity.class, ORDER_ID));
                tx.commit();
            } catch (RuntimeException | Error failure) {
                if (tx.isActive()) tx.rollback();
                throw failure;
            }
        }
        try (EntityManager em = factory.createEntityManager()) {
            assertNull(em.find(OrderEntity.class, ORDER_ID));
            assertNull(em.find(OrderItemEntity.class, ITEM_A));
            assertNull(em.find(OrderItemEntity.class, ITEM_B));
            assertNotNull(em.find(ProductEntity.class, PRODUCT_ID));
            assertNotNull(em.find(CustomerEntity.class, CUSTOMER_A));
        }
    }

    @Test
    void databaseRejectsSecondProfileForSameCustomer() {
        runner.experimentOneToOne();
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            try {
                CustomerEntity customer = em.find(CustomerEntity.class, CUSTOMER_A);
                em.persist(new CustomerProfileEntity(902L, customer, "Duplicate"));
                RuntimeException failure = assertThrows(RuntimeException.class, em::flush);
                Throwable cause = failure;
                while (cause.getCause() != null) cause = cause.getCause();
                java.sql.SQLException sqlFailure = assertInstanceOf(java.sql.SQLException.class, cause);
                assertEquals("23505", sqlFailure.getSQLState(), "PostgreSQL unique_violation");
            } finally {
                if (tx.isActive()) tx.rollback();
            }
        }
    }
}
