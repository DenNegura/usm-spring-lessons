-- Отдельная schema пятой лекции; выполняется вручную, Hibernate только validate.
BEGIN;
CREATE SCHEMA IF NOT EXISTS lesson5;
SET LOCAL search_path TO lesson5;
CREATE TABLE IF NOT EXISTS customers (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address_city VARCHAR(100),
    address_street VARCHAR(255)
);
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    subtotal NUMERIC(12, 2) NOT NULL,
    total NUMERIC(12, 2) NOT NULL,
    customer_id BIGINT NOT NULL REFERENCES customers(id)
);
CREATE TABLE IF NOT EXISTS products (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(12, 2) NOT NULL
);
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0)
);
CREATE TABLE IF NOT EXISTS customer_profiles (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL UNIQUE REFERENCES customers(id),
    description VARCHAR(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS students (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS courses (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS student_courses (
    student_id BIGINT NOT NULL REFERENCES students(id),
    course_id BIGINT NOT NULL REFERENCES courses(id),
    PRIMARY KEY (student_id, course_id)
);
COMMIT;
