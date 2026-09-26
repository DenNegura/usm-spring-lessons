package com.example.orders.demo;

import com.example.orders.entitie.OrderEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;

@Component
public class PersistenceContextDemoRunner implements CommandLineRunner {
    private static final long ID = 201;
    private static final long REMOVE_ID = 202;
    private static final int BATCH_SIZE = 5;
    private static final BigDecimal CHANGED_TOTAL = new BigDecimal("999.00");
    private final EntityManagerFactory factory;
    private final DemoDataHelper data;
    private final PersistenceDemoProperties properties;

    public PersistenceContextDemoRunner(EntityManagerFactory factory, DemoDataHelper data,
                                        PersistenceDemoProperties properties) {
        this.factory = factory;
        this.data = data;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        String experiment = properties.persistenceExperiment();
        if (experiment == null) experiment = "";
        switch (experiment) {
            case "transaction-rollback" -> experimentFlushAndRollback();
            case "identity-cache" -> experimentIdentityAndFirstLevelCache();
            case "dirty-checking" -> experimentDirtyChecking();
            case "detach" -> experimentDetach();
            case "merge" -> experimentMerge();
            case "remove" -> experimentRemove();
            case "batch" -> experimentBatchFlushClear();
            default -> {
                System.out.println("Select app.demo.persistence-experiment:");
                System.out.println("transaction-rollback, identity-cache, dirty-checking, detach, merge, remove, batch");
                return;
            }
        }
        System.out.println("=== PASS: " + experiment + " ===");
    }

    private static void check(boolean condition, String message) {
        System.out.println(message + " -> " + condition);
        if (!condition) throw new IllegalStateException(message);
    }

    private static void section(String text) {
        System.out.println("\n=== " + text + " ===");
    }

    public void experimentFlushAndRollback() {
        data.reset(ID);
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                section("BEFORE");
                OrderEntity order = em.find(OrderEntity.class, ID);
                BigDecimal original = order.getTotal();
                System.out.println("DB total: " + original);
                section("CHANGE MANAGED ENTITY");
                order.changeTotal(CHANGED_TOTAL);
                System.out.println("Java total: " + order.getTotal());
                section("FLUSH: observe UPDATE below");
                em.flush();
                section("ROLLBACK");
                transaction.rollback();
                // rollback работает с database transaction. Он не обязан возвращать поля
                // существующего Java object к старым значениям; проверяем БД в новом context.
                System.out.println("Java total after rollback: " + order.getTotal());
            } catch (RuntimeException | Error failure) {
                if (transaction.isActive()) transaction.rollback();
                throw failure;
            }
        }
        data.verify(ID, DemoDataHelper.INITIAL_TOTAL);
    }

    public void experimentIdentityAndFirstLevelCache() {
        data.reset(ID);
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                section("FIRST FIND");
                OrderEntity first = em.find(OrderEntity.class, ID);
                section("SECOND FIND IN SAME PERSISTENCE CONTEXT");
                OrderEntity second = em.find(OrderEntity.class, ID);
                check(first == second, "first == second");
                System.out.println("identityHashCode(first): " + System.identityHashCode(first));
                System.out.println("identityHashCode(second): " + System.identityHashCode(second));
                check(em.contains(first), "contains(first)");
                check(em.contains(second), "contains(second)");
                section("CLEAR PERSISTENCE CONTEXT");
                em.clear();
                System.out.println("contains(first): " + em.contains(first));
                check(!em.contains(first) && !em.contains(second), "old references detached");
                section("FIND AFTER CLEAR");
                OrderEntity third = em.find(OrderEntity.class, ID);
                System.out.println("first == third: " + (first == third));
                System.out.println("identityHashCode(third): " + System.identityHashCode(third));
                check(first != third && em.contains(third), "new managed instance");
                transaction.commit();
                // В application-managed EntityManager commit не закрывает Persistence Context.
                check(em.contains(third), "contains(third) after commit");
            } catch (RuntimeException | Error failure) {
                if (transaction.isActive()) transaction.rollback();
                throw failure;
            }
        }
    }

    public void experimentDirtyChecking() {
        data.reset(ID);
        updateUsingManagedEntity(ID, CHANGED_TOTAL);
        data.verify(ID, CHANGED_TOTAL);
    }

    public void updateUsingManagedEntity(long id, BigDecimal newTotal) {
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                section("FIND MANAGED ENTITY");
                OrderEntity order = em.find(OrderEntity.class, id);
                check(em.contains(order), "contains(order)");
                section("CHANGE ORDINARY JAVA OBJECT");
                order.changeTotal(newTotal);
                // Dirty checking: order MANAGED в текущем Persistence Context.
                // merge для managed Entity не нужен. Здесь нет save/update/merge.
                section("COMMIT: implicit flush executes UPDATE");
                transaction.commit();
            } catch (RuntimeException | Error failure) {
                if (transaction.isActive()) transaction.rollback();
                throw failure;
            }
        }
    }

    public void experimentDetach() {
        data.reset(ID);
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                OrderEntity order = em.find(OrderEntity.class, ID);
                section("MANAGED");
                check(em.contains(order), "contains(order)");
                section("DETACH");
                // Намеренно прекращаем tracking: дальнейшая локальная обработка не меняет БД.
                em.detach(order);
                System.out.println("contains(order): " + em.contains(order));
                check(!em.contains(order), "detached");
                section("CHANGE DETACHED OBJECT");
                order.changeTotal(CHANGED_TOTAL);
                section("COMMIT");
                transaction.commit();
            } catch (RuntimeException | Error failure) {
                if (transaction.isActive()) transaction.rollback();
                throw failure;
            }
        }
        data.verify(ID, DemoDataHelper.INITIAL_TOTAL);
    }

    public void experimentMerge() {
        data.reset(ID);
        OrderEntity detached;
        section("PHASE 1: FIRST CONTEXT");
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                detached = em.find(OrderEntity.class, ID);
                check(em.contains(detached), "contains before commit");
                transaction.commit();
                check(em.contains(detached), "contains after commit, before close");
            } catch (RuntimeException | Error failure) {
                if (transaction.isActive()) transaction.rollback();
                throw failure;
            }
        }
        section("FIRST ENTITY MANAGER CLOSED");
        // close завершил context, но Java object продолжает существовать.
        // contains на закрытом EntityManager запрещён; проверяем в новом.
        detached.changeTotal(CHANGED_TOTAL);
        section("PHASE 2: NEW CONTEXT");
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                System.out.println("contains(detached) before merge: " + em.contains(detached));
                // merge получает managed representation и копирует state. Исходный объект
                // остаётся detached. После merge продолжаем работать с managed.
                OrderEntity managed = em.merge(detached);
                System.out.println("contains(detached) after merge: " + em.contains(detached));
                System.out.println("contains(managed): " + em.contains(managed));
                System.out.println("detached == managed: " + (detached == managed));
                System.out.println("identityHashCode(detached): " + System.identityHashCode(detached));
                System.out.println("identityHashCode(managed): " + System.identityHashCode(managed));
                check(!em.contains(detached) && em.contains(managed) && detached != managed,
                        "state copied into different managed instance");
                managed.changeTotal(new BigDecimal("777.00"));
                detached.changeTotal(new BigDecimal("555.00"));
                section("COMMIT: database should receive 777.00");
                transaction.commit();
            } catch (RuntimeException | Error failure) {
                if (transaction.isActive()) transaction.rollback();
                throw failure;
            }
        }
        data.verify(ID, new BigDecimal("777.00"));
    }

    public void experimentRemove() {
        data.reset(REMOVE_ID);
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                section("FIND MANAGED ENTITY");
                OrderEntity order = em.find(OrderEntity.class, REMOVE_ID);
                check(em.contains(order), "contains before remove");
                section("REMOVE: DELETE scheduled");
                em.remove(order);
                System.out.println("contains after remove: " + em.contains(order));
                // REMOVED != DETACHED: DELETE запланирован. contains возвращает false
                // и для removed; один contains не является детектором всех lifecycle states.
                check(!em.contains(order), "removed is not reported as managed by contains");
                section("COMMIT");
                transaction.commit();
            } catch (RuntimeException | Error failure) {
                if (transaction.isActive()) transaction.rollback();
                throw failure;
            }
        }
        data.verify(REMOVE_ID, null);
        // Следующий запуск восстановит строку в PREPARE, сейчас оставляем её отсутствующей.
    }

    public void experimentBatchFlushClear() {
        data.clearBatchRows();
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                section("BATCH: one transaction, 23 entities, chunk size 5");
                for (int i = 1; i <= 23; i++) {
                    OrderEntity order = DemoDataHelper.newOrder(2200L + i);
                    check(!em.contains(order), "NEW contains == false, id=" + order.getId());
                    em.persist(order);
                    check(em.contains(order), "MANAGED contains == true");
                    if (i % BATCH_SIZE == 0) {
                        section("FLUSH + CLEAR at " + i);
                        // Сначала flush: clear до синхронизации может потерять pending state.
                        em.flush();
                        em.clear();
                        check(!em.contains(order), "DETACHED after clear");
                    }
                }
                section("FLUSH + CLEAR final partial chunk (3 entities)");
                em.flush();
                em.clear();
                section("COMMIT all 23 inserts");
                transaction.commit();
            } catch (RuntimeException | Error failure) {
                if (transaction.isActive()) transaction.rollback();
                throw failure;
            }
        }
        data.verifyBatch();
    }
}
