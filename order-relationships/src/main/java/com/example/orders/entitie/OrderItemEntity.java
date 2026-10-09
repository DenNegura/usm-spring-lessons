package com.example.orders.entitie;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

// Связь имеет собственное quantity, поэтому это Entity, а не чистая ManyToMany.
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemEntity {
    @Id
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    // Вызывается helper-методами Order в том же пакете.
    @Setter(AccessLevel.PACKAGE)
    private OrderEntity order;

    // Product — независимый каталог: REMOVE/ALL здесь удалял бы чужой товар.
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(nullable = false)
    private int quantity;

    public OrderItemEntity(Long id, ProductEntity product, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        this.id = id;
        this.product = java.util.Objects.requireNonNull(product);
        this.quantity = quantity;
    }
}
