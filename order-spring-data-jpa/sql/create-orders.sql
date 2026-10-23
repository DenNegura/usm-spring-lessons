-- Выполняется вручную. Подходит и для пустой БД, и для схемы предыдущего урока.
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    subtotal NUMERIC(12,2) NOT NULL,
    total NUMERIC(12,2) NOT NULL
);
CREATE TABLE IF NOT EXISTS customers (id BIGINT PRIMARY KEY, name VARCHAR(255) NOT NULL);
CREATE TABLE IF NOT EXISTS products (id BIGINT PRIMARY KEY, name VARCHAR(255) NOT NULL);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_id BIGINT REFERENCES customers(id);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS version BIGINT;
-- Только миграция старых строк; новые версии устанавливает Hibernate.
UPDATE orders SET version = 0 WHERE version IS NULL;
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL
);
