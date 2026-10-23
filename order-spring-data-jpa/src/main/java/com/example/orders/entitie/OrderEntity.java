package com.example.orders.entitie;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "orders")
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

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerEntity customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> items = new ArrayList<>();

    public Long getVersion() { return version; }
    public CustomerEntity getCustomer() { return customer; }
    public List<OrderItemEntity> getItems() { return items; }
    public void changeCustomer(CustomerEntity customer) { this.customer = customer; }
    public void addItem(OrderItemEntity item) {
        items.add(item);
        item.assignOrder(this);
    }

    protected OrderEntity() {
        // требуется JPA provider
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

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void changeTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getTotal() {
        return total;
    }
}