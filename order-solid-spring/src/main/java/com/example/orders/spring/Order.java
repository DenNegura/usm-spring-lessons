package com.example.orders.spring;

import java.math.BigDecimal;

public record Order(
        long id,
        String email,
        String phone,
        BigDecimal subtotal,
        BigDecimal total
) {}

/*
record — компактная форма неизменяемого носителя данных. subtotal хранит сумму до скидки,
total — итог после применения бизнес-правила.
*/
