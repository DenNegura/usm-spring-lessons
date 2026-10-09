package com.example.orders.entitie;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import java.math.BigDecimal;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class OrderEntity {

    @Id
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 32)
    private String phone;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    // Владеющая сторона: это поле определяет orders.customer_id.
    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    // Item принадлежит lifecycle заказа; Product — нет.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter(AccessLevel.NONE)
    private final List<OrderItemEntity> items = new ArrayList<>();

    public void setCustomer(CustomerEntity customer) {
        this.customer = Objects.requireNonNull(customer);
    }

    public List<OrderItemEntity> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(OrderItemEntity item) {
        Objects.requireNonNull(item);
        if (item.getOrder() != null && item.getOrder() != this) {
            throw new IllegalArgumentException("An item belongs to one order; create a new item instead");
        }
        if (!items.contains(item)) items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItemEntity item) {
        if (items.remove(item)) {
            // orphanRemoval удалит managed child при flush, а не запишет NULL в NOT NULL FK.
            // Такой item нельзя отдельно persist или переносить в другой заказ.
            item.setOrder(null);
        }
    }

    public OrderEntity(Long id,
                       String email,
                       String phone,
                       BigDecimal subtotal,
                       BigDecimal total) {
        this.id = id;
        this.email = email;
        this.phone = phone;
        this.subtotal = subtotal;
        this.total = total;
    }

    public void changeTotal(BigDecimal total) {
        this.total = total;
    }

}
