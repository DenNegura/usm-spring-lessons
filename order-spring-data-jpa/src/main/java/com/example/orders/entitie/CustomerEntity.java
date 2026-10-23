package com.example.orders.entitie;

import jakarta.persistence.*;

@Entity
@Table(name = "customers")
public class CustomerEntity {
    @Id
    private Long id;
    @Column(nullable = false)
    private String name;

    protected CustomerEntity() {}
    public CustomerEntity(Long id, String name) { this.id = id; this.name = name; }
    public Long getId() { return id; }
    public String getName() { return name; }
}
