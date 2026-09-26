-- Выполнять вручную только для пустой учебной БД. Hibernate только validate.
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    subtotal NUMERIC(12, 2) NOT NULL,
    total NUMERIC(12, 2) NOT NULL
);
