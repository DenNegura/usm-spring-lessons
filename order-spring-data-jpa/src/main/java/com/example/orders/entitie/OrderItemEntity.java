package com.example.orders.entitie;

import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {
    @Id
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;
    @Column(nullable = false)
    private int quantity;

    protected OrderItemEntity() {}
    public OrderItemEntity(Long id, ProductEntity product, int quantity) {
        this.id = id; this.product = product; this.quantity = quantity;
    }
    public void assignOrder(OrderEntity order) { this.order = order; }
    public ProductEntity getProduct() { return product; }
    public int getQuantity() { return quantity; }
}
