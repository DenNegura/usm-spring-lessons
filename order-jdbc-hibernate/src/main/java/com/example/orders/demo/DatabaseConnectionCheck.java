package com.example.orders.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@Order(0)
public class DatabaseConnectionCheck implements CommandLineRunner {

    private final DataSource dataSource;

    public DatabaseConnectionCheck(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            var metadata = connection.getMetaData();
            System.out.println("DB: " + metadata.getDatabaseProductName());
            System.out.println("URL: " + metadata.getURL());
        }
    }
}