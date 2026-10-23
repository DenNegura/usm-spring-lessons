package com.example.orders.repository;

import com.example.orders.entitie.OrderEntity;
import com.example.orders.projection.OrderSummary;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

// Реализацию создаёт Spring Data; под ней остаётся знакомый EntityManager.
public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByCustomerId(Long customerId);
    List<OrderEntity> findByTotalGreaterThan(BigDecimal total);

    @Query("select o from OrderEntity o where o.customer.id = :customerId")
    List<OrderEntity> findOrdersForCustomer(@Param("customerId") Long customerId);

    @Query("select distinct o from OrderEntity o left join fetch o.items")
    List<OrderEntity> findAllWithItems();

    @EntityGraph(attributePaths = "items")
    @Query("select o from OrderEntity o")
    List<OrderEntity> findAllWithItemsGraph();

    @Query("""
            select new com.example.orders.projection.OrderSummary(o.id, o.customer.name, o.total)
            from OrderEntity o
            """)
    List<OrderSummary> findOrderSummaries();
}
