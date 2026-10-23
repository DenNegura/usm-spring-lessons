package com.example.orders.demo;

import com.example.orders.entitie.*;
import com.example.orders.repository.*;
import com.example.orders.service.OrderApplicationService;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class SpringDataExperiments {

    private final OrderJpaRepository orders;
    private final ProductJpaRepository products;
    private final OrderApplicationService service;
    private final EntityManagerFactory factory;
    private final TransactionTemplate tx;

    public SpringDataExperiments(
        OrderJpaRepository orders,
        ProductJpaRepository products,
        OrderApplicationService service,
        EntityManagerFactory factory,
        PlatformTransactionManager manager
    ) {
        this.orders = orders;
        this.products = products;
        this.service = service;
        this.factory = factory;
        this.tx = new TransactionTemplate(manager);
    }

    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
        System.out.println("Verified: " + message);
    }

    private static void section(String title) {
        System.out.println("=== " + title + " ===");
    }

    public void basic() {
        check(orders.findById(6001L).isPresent(), "findById");
        System.out.println("findAll size: " + orders.findAll().size());
        System.out.println("count: " + orders.count());
        check(orders.existsById(6001L), "existsById");
    }

    public void save() {
        section("NEW: save -> persist");
        OrderEntity fresh = DemoDataSupport.newOrder(6006L);
        OrderEntity saved = orders.save(fresh);
        check(
            saved == fresh && saved.getVersion() != null,
            "persist keeps instance; version initialized"
        );
        section("DETACHED: save -> merge");
        saved.changeTotal(new BigDecimal("175.00"));
        OrderEntity merged = orders.save(saved);
        check(
            merged != saved,
            "merge returns another instance in a new context"
        );
        verifyTotal(6006L, "175.00");
    }

    public void assignedId() {
        section("NEW ENTITY WITH ASSIGNED ID");
        var product = new ProductEntity(null, "Pen");
        System.out.println("Product: id != null, no @Version -> isNew=false");
        section("repository.save()");
        section("OBSERVE SQL");
        var merged = products.save(product);
        check(merged != product, "Product: merge, SELECT then INSERT");
        var order = DemoDataSupport.newOrder(6006L);
        System.out.println("Order: id != null, version == null -> isNew=true");
        check(
            orders.save(order) == order,
            "Order: persist despite assigned id"
        );
    }

    public void derived() {
        check(orders.findByCustomerId(6201L).size() == 5, "findByCustomerId");
        check(
            orders
                .findByTotalGreaterThan(new BigDecimal("99"))
                .stream()
                .filter(o -> o.getId() >= 6001 && o.getId() <= 6005)
                .count() == 5,
            "findByTotalGreaterThan"
        );
    }

    public void jpql() {
        check(
            orders.findOrdersForCustomer(6201L).size() == 5,
            "JPQL customer.id"
        );
    }

    @Transactional
    public void fetchBasic() {
        section("LOAD ORDER");
        var order = orders.findById(6001L).orElseThrow();
        check(
            !Hibernate.isInitialized(order.getItems()),
            "items not initialized"
        );
        section("ACCESS ORDER ITEMS");
        check(order.getItems().size() == 1, "items loaded on access");
        section("ACCESS CUSTOMER NAME");
        System.out.println(order.getCustomer().getName());
    }

    public void lazyOutside() {
        var order = orders.findById(6001L).orElseThrow();
        check(
            !Hibernate.isInitialized(order.getItems()),
            "lazy collection still unloaded"
        );
        section("PERSISTENCE CONTEXT CLOSED");
        try {
            order.getItems().size();
            throw new IllegalStateException(
                "Expected LazyInitializationException"
            );
        } catch (LazyInitializationException expected) {
            System.out.println(
                "Expected: " + expected.getClass().getSimpleName()
            );
        }
    }

    // Каждый вызов получает новый context: предыдущая загрузка не маскирует N+1.
    private void loadAndAccess(
        Supplier<List<OrderEntity>> query,
        boolean fetched
    ) {
        tx.executeWithoutResult(status -> {
            section("LOAD ORDERS");
            var list = query.get();
            var demo = list
                .stream()
                .filter(o -> o.getId() >= 6001 && o.getId() <= 6005)
                .toList();
            check(demo.size() == 5, "five demo orders");
            for (var order : demo)
                check(
                    Hibernate.isInitialized(order.getItems()) == fetched,
                    "fetch plan for " + order.getId()
                );
            section("ACCESS ORDER ITEMS");
            for (var order : demo)
                check(order.getItems().size() == 1, "items " + order.getId());
        });
    }

    public void nPlusOne() {
        loadAndAccess(orders::findAll, false);
    }

    public void joinFetch() {
        section("NORMAL QUERY");
        nPlusOne();
        section("JOIN FETCH QUERY (new context)");
        loadAndAccess(orders::findAllWithItems, true);
    }

    public void entityGraph() {
        loadAndAccess(orders::findAllWithItemsGraph, true);
    }

    public void transactional(boolean rollback) {
        try {
            service.changeCustomerAndCreateOrder(rollback);
            check(!rollback, "commit completed");
        } catch (OrderApplicationService.DemoRollbackException expected) {
            check(rollback, "expected RuntimeException escaped service proxy");
        }
        section("VERIFY DATABASE");
        verifyTotal(6001L, rollback ? "100.00" : "250.00");
        check(
            orders.existsById(6006L) != rollback,
            "second order commit/rollback"
        );
        check(
            orders
                .findByCustomerId(rollback ? 6201L : 6202L)
                .stream()
                .anyMatch(o -> o.getId() == 6001L),
            "customer change commit/rollback"
        );
    }

    private void verifyTotal(long id, String total) {
        check(
            orders
                .findById(id)
                .orElseThrow()
                .getTotal()
                .compareTo(new BigDecimal(total)) == 0,
            "database total=" + total
        );
    }

    public void projection() {
        var summaries = orders.findOrderSummaries();
        summaries.forEach(System.out::println);
        check(
            summaries
                .stream()
                .filter(s -> s.id() >= 6001 && s.id() <= 6005)
                .allMatch(s -> s.customerName().equals("Anna")),
            "DTO customerName"
        );
        check(
            summaries
                .stream()
                .filter(s -> s.id() >= 6001 && s.id() <= 6005)
                .count() == 5,
            "five DTO rows"
        );
    }

    public void optimisticLocking() {
        // Только здесь намеренно возвращаемся к двум application-managed context.
        try (
            EntityManager a = factory.createEntityManager();
            EntityManager b = factory.createEntityManager()
        ) {
            var ta = a.getTransaction();
            var tb = b.getTransaction();
            try {
                ta.begin();
                tb.begin();
                var first = a.find(OrderEntity.class, 6001L);
                var second = b.find(OrderEntity.class, 6001L);
                long initial = first.getVersion();
                check(
                    second.getVersion().equals(first.getVersion()),
                    "A and B loaded same version=" + initial
                );
                first.changeTotal(new BigDecimal("120.00"));
                section("COMMIT A");
                ta.commit();
                check(
                    first.getVersion() == initial + 1,
                    "version incremented by Hibernate"
                );
                second.changeTotal(new BigDecimal("130.00"));
                section("COMMIT B: EXPECT CONFLICT");
                boolean conflict = false;
                try {
                    tb.commit();
                } catch (RuntimeException failure) {
                    for (
                        Throwable cause = failure;
                        cause != null;
                        cause = cause.getCause()
                    ) {
                        if (cause instanceof OptimisticLockException) conflict =
                            true;
                    }
                    if (!conflict) throw failure;
                    System.out.println("Expected OptimisticLockException");
                }
                check(conflict, "stale version rejected");
            } finally {
                if (ta.isActive()) ta.rollback();
                if (tb.isActive()) tb.rollback();
            }
        }
        verifyTotal(6001L, "120.00");
    }
}
