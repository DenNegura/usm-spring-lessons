package com.example.orders.entitie;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

// Значение внутри Customer: нет собственного id, таблицы и lifecycle Entity.
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Address {

    @Column(name = "address_city", length = 100)
    private String city;

    @Column(name = "address_street", length = 255)
    private String street;

}
