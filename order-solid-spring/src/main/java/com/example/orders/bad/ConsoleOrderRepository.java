package com.example.orders.bad;

public class ConsoleOrderRepository {
    public void save(Order order) {
        System.out.println(
                "ORDER SAVED -> id=" + order.id()
                        + ", total=" + order.total()
        );
    }
}

/*
* На первом занятии мы не работаем с базой данных и файлами.
* Метод save только печатает сообщение в консоль и тем самым имитирует сохранение.
* Это позволяет сосредоточиться на связях между классами.
* */