package com.example.orders.entitie;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    protected ProductEntity() {}

    public ProductEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
