package com.example.orders;

import com.example.orders.demo.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

// Настоящая PostgreSQL и validate: никаких подмен диалекта или автосоздания схемы.
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpringDataIntegrationTest {
    private ConfigurableApplicationContext context;
    private DemoDataSupport data;
    private SpringDataExperiments experiments;

    @BeforeAll
    void start() {
        context = SpringApplication.run(OrderApplication.class, "--app.demo.data-experiment=");
        data = context.getBean(DemoDataSupport.class);
        experiments = context.getBean(SpringDataExperiments.class);
    }
    @AfterAll
    void stop() { if (context != null) context.close(); }
    @BeforeEach
    void prepare() { data.reset(); }

    @Test void repositorySaveAndRead() { experiments.save(); }
    @Test void assignedIdDetection() { experiments.assignedId(); }
    @Test void derivedQuery() { experiments.derived(); }
    @Test void jpqlQuery() { experiments.jpql(); }
    @Test void fetchJoinInitializesCollection() { experiments.joinFetch(); }
    @Test void entityGraphInitializesCollection() { experiments.entityGraph(); }
    @Test void lazyOutsideContextFails() { experiments.lazyOutside(); }
    @Test void rollbackCancelsBothChanges() { experiments.transactional(true); }
    @Test void optimisticConflict() { experiments.optimisticLocking(); }
}
