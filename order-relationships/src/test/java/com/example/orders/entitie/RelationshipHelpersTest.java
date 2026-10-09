package com.example.orders.entitie;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class RelationshipHelpersTest {
    @Test
    void customerHelperSynchronizesAndReassignsBothSidesWithoutDuplicates() {
        CustomerEntity a = new CustomerEntity(501L, "A");
        CustomerEntity b = new CustomerEntity(502L, "B");
        OrderEntity order = new OrderEntity();
        a.addOrder(order);
        a.addOrder(order);
        assertEquals(1, a.getOrders().size());
        assertSame(a, order.getCustomer());
        b.addOrder(order);
        assertFalse(a.getOrders().contains(order));
        assertTrue(b.getOrders().contains(order));
        assertSame(b, order.getCustomer());
        assertThrows(NullPointerException.class, () -> order.setCustomer(null));
        b.removeOrder(order, a);
        assertSame(a, order.getCustomer());
        assertTrue(a.getOrders().contains(order));
        assertTrue(b.getOrders().isEmpty());
    }

    @Test
    void itemHelperSynchronizesSidesAndRejectsMovingAnOwnedChild() {
        OrderEntity order = new OrderEntity();
        OrderEntity other = new OrderEntity();
        ProductEntity product = new ProductEntity(701L, "Product", BigDecimal.TEN);
        OrderItemEntity item = new OrderItemEntity(801L, product, 2);
        order.addItem(item);
        order.addItem(item);
        assertEquals(1, order.getItems().size());
        assertSame(order, item.getOrder());
        assertThrows(IllegalArgumentException.class, () -> other.addItem(item));
        assertSame(order, item.getOrder());
        order.removeItem(item);
        assertNull(item.getOrder());
        assertTrue(order.getItems().isEmpty());
        assertSame(product, item.getProduct());
    }
}
