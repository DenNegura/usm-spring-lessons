package com.example.orders.repository;

import com.example.orders.domain.Order;

import java.util.Optional;

import com.example.orders.entitie.OrderEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Profile("jpa")
public class JpaOrderRepository implements OrderRepository {

    private final EntityManagerFactory entityManagerFactory;

    public JpaOrderRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void save(Order order) {
        try (EntityManager entityManager =
                     entityManagerFactory.createEntityManager()) {

            EntityTransaction transaction =
                    entityManager.getTransaction();

            try {
                transaction.begin();
                entityManager.persist(toEntity(order));
                transaction.commit();
            } catch (RuntimeException e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            }
        }
    }

    @Override
    public Optional<Order> findById(long id) {
        try (EntityManager entityManager =
                     entityManagerFactory.createEntityManager()) {

            OrderEntity entity =
                    entityManager.find(OrderEntity.class, id);

            return Optional.ofNullable(entity)
                    .map(this::toDomain);
        }
    }

    private OrderEntity toEntity(Order order) {
        return new OrderEntity(
                order.id(),
                order.email(),
                order.phone(),
                order.subtotal(),
                order.total()
        );
    }

    private Order toDomain(OrderEntity entity) {
        return new Order(
                entity.getId(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getSubtotal(),
                entity.getTotal()
        );
    }
}