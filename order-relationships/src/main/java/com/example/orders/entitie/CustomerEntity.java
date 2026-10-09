package com.example.orders.entitie;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Embedded;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "customers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerEntity {
    @Id
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Embedded
    @Setter
    private Address address;

    // mappedBy указывает Java-поле OrderEntity.customer, НЕ колонку customer_id.
    @OneToMany(mappedBy = "customer")
    private final List<OrderEntity> orders = new ArrayList<>();

    public CustomerEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    // Две Java-стороны надо согласовать самим; Hibernate не исправляет коллекции.
    public void addOrder(OrderEntity order) {
        Objects.requireNonNull(order);
        CustomerEntity previous = order.getCustomer();
        if (previous != null && previous != this) previous.orders.remove(order);
        if (!orders.contains(order)) orders.add(order);
        order.setCustomer(this);
    }
    // Обязательный FK не позволяет просто обнулить customer: передаём заказ другому покупателю.
    public void removeOrder(OrderEntity order, CustomerEntity newCustomer) {
        Objects.requireNonNull(newCustomer);
        if (newCustomer == this) throw new IllegalArgumentException("Choose another customer");
        if (order.getCustomer() != this || !orders.contains(order)) {
            throw new IllegalArgumentException("Order does not belong to this customer");
        }
        newCustomer.addOrder(order);
    }
}
