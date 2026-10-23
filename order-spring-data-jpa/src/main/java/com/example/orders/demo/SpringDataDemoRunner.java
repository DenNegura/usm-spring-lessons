package com.example.orders.demo;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SpringDataDemoRunner implements CommandLineRunner {

    public static final List<String> EXPERIMENTS = List.of(
        "repository-basic",
        "repository-save",
        "save-assigned-id",
        "derived-query",
        "jpql-query",
        "fetch-basic",
        "lazy-outside-context",
        "n-plus-one",
        "join-fetch",
        "entity-graph",
        "transactional-service",
        "transactional-rollback",
        "dto-projection",
        "optimistic-locking"
    );
    private final SpringDataDemoProperties properties;
    private final DemoDataSupport data;
    private final SpringDataExperiments experiments;

    public SpringDataDemoRunner(
        SpringDataDemoProperties properties,
        DemoDataSupport data,
        SpringDataExperiments experiments
    ) {
        this.properties = properties;
        this.data = data;
        this.experiments = experiments;
    }

    @Override
    public void run(String... args) {
        String selected = properties.dataExperiment();
        if (!EXPERIMENTS.contains(selected == null ? "" : selected)) {
            System.out.println(
                "Select app.demo.data-experiment: " + EXPERIMENTS
            );
            return;
        }
        data.reset();
        System.out.println("start experiment: " + selected);
        switch (selected) {
            case "repository-basic" -> experiments.basic();
            case "repository-save" -> experiments.save();
            case "save-assigned-id" -> experiments.assignedId();
            case "derived-query" -> experiments.derived();
            case "jpql-query" -> experiments.jpql();
            case "fetch-basic" -> experiments.fetchBasic();
            case "lazy-outside-context" -> experiments.lazyOutside();
            case "n-plus-one" -> experiments.nPlusOne();
            case "join-fetch" -> experiments.joinFetch();
            case "entity-graph" -> experiments.entityGraph();
            case "transactional-service" -> experiments.transactional(false);
            case "transactional-rollback" -> experiments.transactional(true);
            case "dto-projection" -> experiments.projection();
            case "optimistic-locking" -> experiments.optimisticLocking();
            default -> throw new IllegalStateException(selected);
        }
        System.out.println("=== PASS: " + selected + " ===");
    }
}
