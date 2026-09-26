package com.example.orders.entitie;

import jakarta.persistence.Column;
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