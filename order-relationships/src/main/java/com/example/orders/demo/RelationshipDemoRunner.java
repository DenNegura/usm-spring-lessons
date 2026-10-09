package com.example.orders.demo;

import com.example.orders.entitie.Address;
import com.example.orders.entitie.CourseEntity;
import com.example.orders.entitie.CustomerEntity;
import com.example.orders.entitie.CustomerProfileEntity;
import com.example.orders.entitie.OrderEntity;
import com.example.orders.entitie.OrderItemEntity;
import com.example.orders.entitie.ProductEntity;
import com.example.orders.entitie.StudentEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import static com.example.orders.demo.RelationshipDemoDataHelper.CUSTOMER_A;
import static com.example.orders.demo.RelationshipDemoDataHelper.CUSTOMER_B;
import static com.example.orders.demo.RelationshipDemoDataHelper.ORDER_ID;
import static com.example.orders.demo.RelationshipDemoDataHelper.PRODUCT_ID;
import static com.example.orders.demo.RelationshipDemoDataHelper.ITEM_A;
import static com.example.orders.demo.RelationshipDemoDataHelper.ITEM_B;
import static com.example.orders.demo.RelationshipDemoDataHelper.PROFILE_ID;
import static com.example.orders.demo.RelationshipDemoDataHelper.STUDENT_ID;
import static com.example.orders.demo.RelationshipDemoDataHelper.COURSE_ID;

@Component
public class RelationshipDemoRunner implements CommandLineRunner {
    private final EntityManagerFactory factory;
    private final RelationshipDemoDataHelper data;
    private final RelationshipDemoProperties properties;

    public RelationshipDemoRunner(EntityManagerFactory factory, RelationshipDemoDataHelper data,
                                  RelationshipDemoProperties properties) {
        this.factory = factory;
        this.data = data;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        String experiment = properties.relationshipExperiment();
        if (experiment == null) experiment = "";
        section("RELATIONSHIP EXPERIMENT: " + experiment);
        switch (experiment) {
            case "many-to-one" -> experimentManyToOne();
            case "owning-side" -> experimentOwningSide();
            case "cascade-persist" -> experimentCascadePersist();
            case "orphan-removal" -> experimentOrphanRemoval();
            case "one-to-one" -> experimentOneToOne();
            case "many-to-many" -> experimentManyToMany();
            case "embeddable" -> experimentEmbeddable();
            default -> {
                System.out.println("Select app.demo.relationship-experiment:");
                System.out.println("many-to-one, owning-side, cascade-persist, orphan-removal, one-to-one, many-to-many, embeddable");
                return;
            }
        }
        section("PASS: " + experiment);
    }

    static void section(String text) { System.out.println("\n=== " + text + " ==="); }
    static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
        System.out.println("Verified: " + message);
    }

    public void experimentManyToOne() {
        data.prepare();
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                section("LOAD CUSTOMER");
                CustomerEntity customer = em.find(CustomerEntity.class, CUSTOMER_A);
                section("CREATE ORDER");
                OrderEntity order = RelationshipDemoDataHelper.newOrder(ORDER_ID);
                section("SET RELATIONSHIP");
                order.setCustomer(customer);
                System.out.println("order.customer.id = " + order.getCustomer().getId());
                section("PERSIST ORDER: INSERT must contain customer_id");
                em.persist(order);
                section("FLUSH / COMMIT");
                tx.commit();
            } catch (RuntimeException | Error failure) {
                if (tx.isActive()) tx.rollback();
                throw failure;
            }
        }
        data.verifyCustomer(CUSTOMER_A);
    }

    public void experimentOwningSide() {
        data.prepareOrder(false);
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                section("BEFORE: Order belongs to Customer A");
                OrderEntity order = em.find(OrderEntity.class, ORDER_ID);
                CustomerEntity customerB = em.find(CustomerEntity.class, CUSTOMER_B);
                section("CHANGE ONLY INVERSE COLLECTION");
                customerB.getOrders().add(order);
                System.out.println("B.orders contains order = " + customerB.getOrders().contains(order));
                System.out.println("order.customer.id still = " + order.getCustomer().getId());
                System.out.println("Expected: NO customer_id UPDATE");
                section("FLUSH / COMMIT");
                tx.commit();
            } catch (RuntimeException | Error failure) {
                if (tx.isActive()) tx.rollback();
                throw failure;
            }
        }
        data.verifyCustomer(CUSTOMER_A);
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                section("CHANGE OWNING SIDE IN A NEW CONTEXT");
                OrderEntity order = em.find(OrderEntity.class, ORDER_ID);
                CustomerEntity customerB = em.find(CustomerEntity.class, CUSTOMER_B);
                order.setCustomer(customerB);
                System.out.println("Expected: UPDATE orders, customer_id = 502");
                section("FLUSH / COMMIT");
                tx.commit();
            } catch (RuntimeException | Error failure) {
                if (tx.isActive()) tx.rollback();
                throw failure;
            }
        }
        data.verifyCustomer(CUSTOMER_B);
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                section("HELPER: synchronize both Java sides, move back to A");
                OrderEntity order = em.find(OrderEntity.class, ORDER_ID);
                CustomerEntity customerB = order.getCustomer();
                CustomerEntity customerA = em.find(CustomerEntity.class, CUSTOMER_A);
                customerA.addOrder(order);
                check(order.getCustomer() == customerA && customerA.getOrders().contains(order)
                        && !customerB.getOrders().contains(order), "both Java sides synchronized");
                section("FLUSH / COMMIT");
                tx.commit();
            } catch (RuntimeException | Error failure) {
                if (tx.isActive()) tx.rollback();
                throw failure;
            }
        }
        data.verifyCustomer(CUSTOMER_A);
    }

    public void experimentCascadePersist() {
        data.prepare();
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                section("BEFORE: existing Product, new Order and OrderItem");
                ProductEntity product = em.find(ProductEntity.class, PRODUCT_ID);
                OrderEntity order = RelationshipDemoDataHelper.newOrder(ORDER_ID);
                em.find(CustomerEntity.class, CUSTOMER_A).addOrder(order);
                OrderItemEntity item = new OrderItemEntity(ITEM_A, product, 2);
                section("CHANGE JAVA RELATIONSHIP: addItem helper");
                order.addItem(item);
                check(item.getOrder() == order && order.getItems().contains(item), "both item sides synchronized");
                section("PERSIST ONLY ORDER");
                System.out.println("Expected: INSERT orders + INSERT order_items; NO Product INSERT");
                em.persist(order);
                section("FLUSH / COMMIT");
                tx.commit();
            } catch (RuntimeException | Error failure) {
                if (tx.isActive()) tx.rollback();
                throw failure;
            }
        }
        data.verifyCustomer(CUSTOMER_A);
        data.verifyItems(1);
    }

    public void experimentOrphanRemoval() {
        data.prepareOrder(true);
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                section("BEFORE: two order items");
                OrderEntity order = em.find(OrderEntity.class, ORDER_ID);
                check(order.getItems().size() == 2, "two initial items");
                OrderItemEntity item = order.getItems().stream()
                        .filter(candidate -> candidate.getId() == ITEM_A).findFirst().orElseThrow();
                section("CHANGE JAVA RELATIONSHIP: removeItem, not em.remove(order)");
                order.removeItem(item);
                check(item.getOrder() == null && !order.getItems().contains(item), "both Java item sides cleared");
                System.out.println("Expected: DELETE order_items id=801, Product remains");
                section("FLUSH / COMMIT");
                tx.commit();
            } catch (RuntimeException | Error failure) {
                if (tx.isActive()) tx.rollback();
                throw failure;
            }
        }
        data.verifyItems(1);
        try (EntityManager em = factory.createEntityManager()) {
            check(em.find(OrderItemEntity.class, ITEM_A) == null, "removed child absent");
            check(em.find(OrderItemEntity.class, ITEM_B) != null, "other child remains");
            check(em.find(OrderEntity.class, ORDER_ID) != null, "parent remains");
        }
    }

    public void experimentOneToOne() {
        data.prepare();
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                section("BEFORE: existing customer without profile");
                CustomerEntity customer = em.find(CustomerEntity.class, CUSTOMER_A);
                section("CHANGE JAVA RELATIONSHIP: profile.customer");
                CustomerProfileEntity profile = new CustomerProfileEntity(PROFILE_ID, customer, "Demo profile");
                System.out.println("Expected: INSERT customer_profiles(customer_id, description, id)");
                em.persist(profile);
                section("FLUSH / COMMIT");
                tx.commit();
            } catch (RuntimeException | Error failure) {
                if (tx.isActive()) tx.rollback();
                throw failure;
            }
        }
        section("VERIFY DATABASE: new EntityManager, FK + UNIQUE");
        try (EntityManager em = factory.createEntityManager()) {
            CustomerProfileEntity profile = em.find(CustomerProfileEntity.class, PROFILE_ID);
            check(profile.getCustomer().getId() == CUSTOMER_A, "profile references customer 501");
            long uniqueCount = ((Number) em.createNativeQuery("""
                    SELECT count(*) FROM pg_constraint c
                    JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
                    WHERE c.conrelid = 'customer_profiles'::regclass AND c.contype = 'u'
                      AND cardinality(c.conkey) = 1 AND a.attname = 'customer_id'
                    """).getSingleResult()).longValue();
            check(uniqueCount == 1, "physical UNIQUE on customer_profiles.customer_id");
        }
    }

    public void experimentManyToMany() {
        data.prepare();
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                section("BEFORE: existing Student and Course");
                StudentEntity student = em.find(StudentEntity.class, STUDENT_ID);
                CourseEntity course = em.find(CourseEntity.class, COURSE_ID);
                section("CHANGE JAVA RELATIONSHIP: student.courses");
                student.addCourse(course);
                System.out.println("Expected: INSERT student_courses(student_id, course_id)");
                section("FLUSH / COMMIT");
                tx.commit();
            } catch (RuntimeException | Error failure) {
                if (tx.isActive()) tx.rollback();
                throw failure;
            }
        }
        section("VERIFY DATABASE: new EntityManager");
        try (EntityManager em = factory.createEntityManager()) {
            long count = ((Number) em.createNativeQuery("""
                    SELECT count(*) FROM student_courses WHERE student_id = 1001 AND course_id = 1101
                    """).getSingleResult()).longValue();
            check(count == 1, "join table stores student_id=1001, course_id=1101");
            check(em.find(StudentEntity.class, STUDENT_ID).getCourses().stream()
                    .anyMatch(course -> course.getId() == COURSE_ID), "Java collection restored from join table");
        }
    }

    public void experimentEmbeddable() {
        data.prepare();
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                section("BEFORE: customer without address");
                CustomerEntity customer = em.find(CustomerEntity.class, CUSTOMER_A);
                section("CHANGE JAVA STATE: embedded Address");
                customer.setAddress(new Address("Chisinau", "Student Street"));
                System.out.println("Expected: UPDATE customers, address_city/address_street; NO address table INSERT");
                section("FLUSH / COMMIT");
                tx.commit();
            } catch (RuntimeException | Error failure) {
                if (tx.isActive()) tx.rollback();
                throw failure;
            }
        }
        section("VERIFY DATABASE: new EntityManager");
        try (EntityManager em = factory.createEntityManager()) {
            Object[] row = (Object[]) em.createNativeQuery("""
                    SELECT address_city, address_street FROM customers WHERE id = 501
                    """).getSingleResult();
            check("Chisinau".equals(row[0]) && "Student Street".equals(row[1]), "address stored in Customer columns");
            Address address = em.find(CustomerEntity.class, CUSTOMER_A).getAddress();
            check("Chisinau".equals(address.getCity()) && "Student Street".equals(address.getStreet()),
                    "embedded Java value restored");
        }
    }
}
