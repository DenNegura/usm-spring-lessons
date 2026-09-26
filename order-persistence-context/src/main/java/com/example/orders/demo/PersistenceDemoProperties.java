package com.example.orders.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.demo")
public record PersistenceDemoProperties(String persistenceExperiment) {}
