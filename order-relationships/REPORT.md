# Отчёт: отдельный модуль пятой лекции

## Схема базы данных

![Схема базы данных lesson5](database-schema.svg)

[Открыть схему в SVG](database-schema.svg). Поля и ограничения соответствуют
[sql/create-schema.sql](sql/create-schema.sql).

Проверка выполнена 2026-10-09. Модуль: `order-relationships`.

## 1. Существующие модули

Из `order-persistence-context` удалены только изменения Lesson 5, сделанные
в предыдущем ответе. README, pom и PersistenceContextDemoRunner восстановлены;
сущности, runner, SQL и тесты отношений перенесены в новый модуль.
Пользовательские изменения сохранены: публичный конструктор OrderEntity,
выбранный `app.demo.persistence-experiment=batch`, настройки IDE и изменения
`order-jdbc-hibernate`. Файл DatabaseDemoRunner старого модуля не изменялся.
Сборка восстановленного `order-persistence-context`: BUILD SUCCESS.

## 2. Новые файлы

Все новые файлы находятся внутри `order-relationships`:

```text
.gitignore
pom.xml
compose.yaml
README.md
REPORT.md
sql/create-schema.sql
sql/verify-relationships.sql
scripts/verify-relationships.sh
src/main/resources/application.properties
src/main/java/com/example/orders/OrderApplication.java
src/main/java/com/example/orders/demo/DatabaseConnectionCheck.java
src/main/java/com/example/orders/demo/RelationshipDemoProperties.java
src/main/java/com/example/orders/demo/RelationshipDemoDataHelper.java
src/main/java/com/example/orders/demo/RelationshipDemoRunner.java
src/main/java/com/example/orders/entitie/OrderEntity.java
src/main/java/com/example/orders/entitie/CustomerEntity.java
src/main/java/com/example/orders/entitie/ProductEntity.java
src/main/java/com/example/orders/entitie/OrderItemEntity.java
src/main/java/com/example/orders/entitie/CustomerProfileEntity.java
src/main/java/com/example/orders/entitie/StudentEntity.java
src/main/java/com/example/orders/entitie/CourseEntity.java
src/main/java/com/example/orders/entitie/Address.java
src/test/java/com/example/orders/entitie/RelationshipHelpersTest.java
src/test/java/com/example/orders/demo/RelationshipPostgresTest.java
```

## 3. База данных

Используется прежний PostgreSQL 18: `localhost:5432/orders`, пользователь и пароль
`student`. Для изоляции модуль использует отдельную schema `lesson5` через
`currentSchema=lesson5`. Compose подключает прежний контейнер и volume.
Схема создаётся вручную `sql/create-schema.sql`; Hibernate только `validate`.

Созданы восемь таблиц: customers, orders, products, order_items,
customer_profiles, students, courses, student_courses. Проверены шесть FK,
UNIQUE customer_profiles.customer_id, обязательный orders.customer_id,
колонки address_city/address_street и отсутствие таблицы Address.
Снимки структуры и данных `public` до и после всех новых опытов совпадают.

## 4. Отношения

* Customer ↔ Order: ManyToOne / OneToMany; владеет Order.customer.
* Order ↔ OrderItem: ManyToOne / OneToMany; ALL и orphanRemoval на Order.items.
* OrderItem → Product: ManyToOne, без каскада; quantity принадлежит сущности связи.
* CustomerProfile → Customer: OneToOne с физическим UNIQUE FK.
* Student → Course: ManyToMany через student_courses, без каскада удаления курса.
* Customer.address: Embedded Address, без id и отдельной таблицы.

Помощники согласуют обе Java-стороны. При обязательном customer_id удаление заказа
из коллекции означает перенос к другому покупателю через
`removeOrder(order, newCustomer)`; обнуление ссылки запрещено.

## 5. Значения свойства

`app.demo.relationship-experiment`:
`many-to-one`, `owning-side`, `cascade-persist`, `orphan-removal`, `one-to-one`,
`many-to-many`, `embeddable`.
Пустое или неизвестное значение выводит список без записи в БД.
Свойство и runner Lesson 4 в новом модуле отсутствуют.

## 6. Запуск

Из каталога `order-relationships`:

```bash
docker compose up -d
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U student -d orders < sql/create-schema.sql
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U student -d orders < sql/verify-relationships.sql
mvn spring-boot:run -Dspring-boot.run.arguments=--app.demo.relationship-experiment=many-to-one
```

Подставьте любое значение из пункта 5. Все семь отдельных команд приведены
в README. Для автоматической проверки: `bash scripts/verify-relationships.sh`.

## 7. Ожидаемый SQL

| Опыт | Основная фаза |
|---|---|
| many-to-one | SELECT customer; INSERT orders с customer_id |
| owning-side | Только обратная коллекция: без UPDATE; владеющая ссылка: UPDATE FK |
| cascade-persist | INSERT orders + INSERT order_items; без INSERT products |
| orphan-removal | DELETE order_items; без UPDATE order_id на NULL |
| one-to-one | INSERT customer_profiles с customer_id |
| many-to-many | INSERT student_courses(student_id, course_id) |
| embeddable | UPDATE customers, колонки адреса |

## 8. Наблюдавшийся SQL

Каждый опыт отдельно запущен дважды на одной БД, все завершились PASS.
Фактические операции соответствуют таблице выше; SQL подготовки отделён маркерами.
В owning-side первый commit не выполняет UPDATE; второй записывает customer_id=502,
помощник переносит заказ обратно к 501 и вызывает ещё один UPDATE.
Hibernate 7.4.5.Final включает в UPDATE orders также email, phone, subtotal, total;
UPDATE customers включает name вместе с address_city/address_street.
Удаление позиции выполняет DELETE, без записи NULL в обязательный FK.

Логи отдельных запусков:
`/tmp/order-relationships.yldRjG` и `/tmp/order-relationships.2rhE8B`.
Режимы справки проверены снимками schema lesson5 до и после: данные не менялись.
`ddl-auto=validate` проходит, активная schema дополнительно проверена тестом.

## 9. Тестирование

* `mvn clean test`: BUILD SUCCESS; 2 теста прошли, 9 PostgreSQL тестов пропущены.
* `mvn clean test -Drelationships.integration=true`: BUILD SUCCESS; все 11 тестов
  прошли, без ошибок и пропусков. Итоговый лог: `/tmp/lesson5-module-final-tests.log`.
* Проверены помощники, FK, каскад persist, сохранность товара при удалении заказа,
  orphanRemoval, UNIQUE (SQLSTATE 23505), таблица связей и встроенный адрес.
* `sql/verify-relationships.sql`: все физические ограничения подтверждены.
* JVM 25, компиляция `--release 21`; отдельная JVM 21 не проверялась.

## 10. Решения относительно предыдущего задания

По новому указанию создан отдельный Maven-модуль внутри существующего репозитория.
Он не зависит от кода прежних модулей. Изолированная schema позволяет сохранить
старую БД и сделать FK покупателя обязательным. Сервисы и прежние демонстрационные
runner не скопированы: новый модуль посвящён только пятой лекции.

Не добавлены JpaRepository, CrudRepository, derived queries, @Query,
LAZY/EAGER примеры, N+1, JOIN FETCH, EntityGraph, блокировки или @Version.
Нет @Transactional: транзакции выполняются явно через EntityTransaction.
Тема получения связанных данных и следующее лабораторное задание не включены.
