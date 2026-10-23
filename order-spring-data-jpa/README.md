# Урок 6. Spring Data JPA: репозитории, запросы, загрузка связей и транзакции

## 1. Связь с предыдущим проектом

```text
order-persistence-context → order-spring-data-jpa
```

Сохранены Java 21, Spring Boot 4.1.1, Maven, PostgreSQL 18, пакет
`com.example.orders` (включая прежнее имя `entitie`), назначаемые вручную id,
поля заказа `email`, `phone`, `subtotal`, `total`, SQL logging и явная SQL-схема.
Исходный проект не изменяется. Здесь отдельный Maven artifact и отдельная БД.

В исходном проекте есть только `OrderEntity`; связей, `Address` и профиля покупателя
нет. Добавлен необходимый минимум той же предметной области:
`Customer ← Order → OrderItem → Product`. У заказа коллекция `items`, у позиции
ссылки на заказ и товар. Обратные коллекции, профиль и адрес этому уроку не нужны.
Старые JDBC/console repositories, уведомления и runner прошлой лекции в новый
проект не перенесены: за один старт выполняется только выбранный опыт.

## 2. Где находится Spring Data JPA

```text
OrderApplicationService
↓
наш interface OrderJpaRepository
↓
Spring Data JPA proxy / repository implementation
↓
EntityManager
↓
Hibernate
↓
JDBC
↓
PostgreSQL
```

Spring Data JPA не заменяет JPA и Hibernate. `repository.findById(...)` использует
JPA API через реализацию репозитория. Persistence Context, dirty checking и
состояния Entity продолжают работать так же, как при ручном EntityManager.

## 3. Что делает JpaRepository

`OrderJpaRepository extends JpaRepository<OrderEntity, Long>` — интерфейс без
написанной нами реализации. Spring Data создаёт реализацию и подключает её к
EntityManager. Опыт `repository-basic` вызывает `findById`, `findAll`, `count`,
`existsById`. В консоли видны соответствующие SELECT и результаты проверок.
Нет необходимости повторно реализовывать эти операции вручную.

## 4. Что на самом деле делает save()

```text
save(entity) → считается ли Entity новой? → persist() либо merge()
```

Это не универсальный SQL UPSERT. При стандартном определении состояния Spring Data
сначала смотрит nullable поле `@Version`, если оно есть, иначе — id.
В `repository-save` новый Order с `version == null` сохраняется через persist.
Затем объект из завершённого context изменяется и передаётся в save повторно:
merge возвращает другой экземпляр в новом context. Продолжаем работать с
возвращённым значением. У managed Entity внутри бизнес-транзакции изменение
отслеживается Hibernate; обязательного save после каждого setter нет.

## 5. Assigned id и определение новой Entity

`id != null` не доказывает наличие строки в БД. В `save-assigned-id` сравниваются:

- Новый Product с id 6102 без `@Version`: Spring Data выбирает merge.
  Для используемой версии Hibernate наблюдаются SELECT и затем INSERT,
  возвращённый объект отличается от исходного.
- Новый Order с id 6006 и nullable `@Version`: версия ещё null, выбирается persist,
  возвращается тот же экземпляр, Hibernate устанавливает версию.

Мы не устанавливаем версию вручную и не вводим собственный механизм `isNew()`.
Поведение merge при отсутствии строки зависит также от mapping/provider; это
наблюдение конкретного опыта, а не обещание универсального UPSERT.

## 6. Derived queries

`derived-query` вызывает два метода:

```java
List<OrderEntity> findByCustomerId(Long customerId);
List<OrderEntity> findByTotalGreaterThan(BigDecimal total);
```

Имя метода → Spring Data разбирает поля и условие → создаёт запрос → Hibernate
выполняет SQL. `CustomerId` здесь соответствует пути `customer.id`.
Проверяются пять заказов покупателя 6201 и условие `total > 99`.

## 7. @Query и JPQL

`jpql-query` вызывает `findOrdersForCustomer`:

```java
@Query("select o from OrderEntity o where o.customer.id = :customerId")
List<OrderEntity> findOrdersForCustomer(@Param("customerId") Long customerId);
```

JPQL работает с Entity, Java fields и relationships: `OrderEntity`, `o.customer.id`.
Имена таблиц `orders` и колонок `customer_id` относятся к SQL, а не к этому JPQL.
Hibernate переводит запрос в SQL PostgreSQL.

## 8. LAZY и EAGER

В исходной модели не было связей. У новых `@ManyToOne` явно выбран LAZY,
у `Order.items` (`@OneToMany`) оставлен стандартный LAZY. По умолчанию JPA
`@ManyToOne` и `@OneToOne` имеют EAGER, `@OneToMany` и `@ManyToMany` — LAZY.

FetchType описывает требование / стратегию получения состояния,
но не является обещанием конкретной формы SQL. EAGER требует получения состояния,
но не обязательно через JOIN; LAZY — допускает отложенную загрузку, это подсказка
provider, а не обещание отдельного SELECT в каждом случае.

В `fetch-basic` одна транзакция удерживает Persistence Context: сначала загружается
Order, проверяется неинициализированная `items`, затем её `size()` вызывает SELECT.
После этого обращение к имени покупателя показывает загрузку другой связи.
Мы наблюдаем реальное поведение Hibernate, а не выводим форму SQL только из fetch.

## 9. LazyInitializationException

`lazy-outside-context` получает заказ через repository.findById. Транзакция
репозитория завершается, его Persistence Context недоступен. Коллекция ещё не
инициализирована. Вызов `order.getItems().size()` вызывает ожидаемый
`LazyInitializationException`, который перехватывается только как часть опыта.

```text
связь не загружена + Persistence Context закрыт + обращение к связи = исключение
```

Возможные решения выбираются под операцию: прочитать нужные данные в границах
сервиса, задать fetch plan или получить DTO. Сделать всё EAGER — не решение.

## 10. N+1

Сначала запустите `n-plus-one`, только затем `join-fetch`.
`=== LOAD ORDERS ===` отмечает обычный findAll, `=== ACCESS ORDER ITEMS ===` — цикл.
На пяти учебных заказах видны 1 SELECT списка + 5 SELECT коллекций.

N+1 — ситуация, когда после одного запроса списка приложение выполняет
дополнительные запросы для связанных данных каждого элемента. Важна причина
появления запросов, а не только их количество. Покупатель и товар в этом опыте
не читаются, поэтому не добавляют SELECT. JDBC batching и кеши не настроены.

## 11. JOIN FETCH

После наблюдения проблемы запустите `join-fetch`:

```java
@Query("select distinct o from OrderEntity o left join fetch o.items")
List<OrderEntity> findAllWithItems();
```

В одном запуске сначала `=== NORMAL QUERY ===` с 1+5 SELECT, затем
`=== JOIN FETCH QUERY (new context) ===` с одним SELECT через LEFT JOIN.
Каждая часть получает новый Persistence Context: ранее загруженные коллекции
не могут скрыть дополнительные запросы. Перед циклом проверяется инициализация,
обращения к items больше не требуют SQL. DISTINCT относится к результату Entity;
строки SQL при коллекции могут повторять колонки заказа.

## 12. @EntityGraph

`entity-graph` использует отдельный метод, не изменяя обычный findAll:

```java
@EntityGraph(attributePaths = "items")
@Query("select o from OrderEntity o")
List<OrderEntity> findAllWithItemsGraph();
```

JOIN FETCH описывает fetch plan внутри JPQL. `@EntityGraph` задаёт его отдельно от
выражения запроса. В этом опыте Hibernate также выполняет LEFT JOIN, и items уже
инициализированы. Ни один подход не объявляется всегда лучшим.
Проблема N+1 решается осознанным выбором данных для конкретной операции,
а не правилом «сделать всё EAGER».

## 13. @Transactional

```text
Раньше: EntityTransaction → begin() → работа → commit()/rollback()
Теперь: @Transactional → граница метода → repository operations → commit/rollback
```

`transactional-service` вызывает публичный метод отдельного Spring bean
`OrderApplicationService.changeCustomerAndCreateOrder(false)` через proxy.
Он читает покупателя, меняет покупателя и total существующего заказа, сохраняет
второй заказ. Все repository operations участвуют в одной транзакции сервиса.
В старом `OrderService` уже встречалась `jakarta.transaction.Transactional`,
но `JpaOrderRepository` самостоятельно открывал EntityManager и EntityTransaction.
Новая операция действительно использует общий Spring-managed EntityManager:
репозитории участвуют в транзакции сервиса.
В новом сервисе нет ручных begin/commit/rollback. Это автоматизация знакомой границы,
а не новая транзакционная технология. Вызов идёт из другого bean: прямой вызов
метода на `this` не проходит через такой proxy.

Repository = доступ к данным. Service = граница бизнес-операции.
Spring Data уже предоставляет транзакционную семантику стандартным CRUD-методам;
мы не расставляем `@Transactional` на каждом методе интерфейса. Для чтения коллекций
в опытах используется TransactionTemplate — явная граница того же Spring transaction
manager. Для реальной бизнес-операции граница показана аннотацией на сервисе.

## 14. Transaction rollback

`transactional-rollback` вызывает тот же сервис с `true`:

```text
=== TRANSACTION START ===
=== CHANGE DATA ===
=== THROW EXCEPTION ===
=== VERIFY DATABASE ===
```

После изменений выполнен flush: INSERT и UPDATE действительно уходят в БД,
но ещё не подтверждаются. RuntimeException выходит через proxy, Spring откатывает
транзакцию. Исключение ловится снаружи сервиса. Новые операции чтения проверяют:
старый total 100.00 и покупатель 6201 сохранены, заказ 6006 отсутствует.
Это проверка БД после rollback, а не значений оставшегося Java object.

## 15. DTO projection

`dto-projection` использует `OrderSummary(Long id, String customerName, BigDecimal total)`
и JPQL `select new com.example.orders.projection.OrderSummary(...)`.
Hibernate выбирает только нужные поля, соединяя заказ с покупателем.
Entity graph и коллекции позиций для этой операции не загружаются.
Если нужны лишь несколько полей, полный граф Entity получать необязательно.
INNER JOIN по покупателю означает, что заказы без покупателя в этот DTO-запрос
не входят; все пять учебных заказов имеют покупателя.

## 16. @Version

В Order добавлено `@Version private Long version`. Версию устанавливает и увеличивает
Hibernate, сеттера нет. SQL добавляет `version BIGINT`. Для старых строк миграция
однократно заполняет null нулём; это перенос прежних данных, не ручное управление
версиями Entity. Nullable Java type дополнительно позволяет Spring Data распознать
новый Order с уже назначенным id (см. `save-assigned-id`).

## 17. Optimistic locking

`optimistic-locking` использует два независимых EntityManager и EntityTransaction,
последовательно, без потоков. Здесь ручной API намеренно оставлен видимым:

```text
A: load version=0
B: load version=0
A: change, commit → version=1
B: change, commit WHERE version=0 → 0 rows → OptimisticLockException
```

В SQL наблюдайте условие:

```sql
update orders set ..., version=? where id=? and version=?
```

Вторая транзакция не перезаписывает подтверждённый результат первой. После
конфликта новый запрос проверяет total 120.00. Проверяются именно
OptimisticLockException в цепочке причин и увеличение версии, случайная ошибка
не считается успешным воспроизведением конфликта.
JPA также поддерживает pessimistic locks, но они не входят в практический объём проекта.

## 18. Как запустить каждый эксперимент

Из каталога `order-spring-data-jpa`, нужны Maven, JDK 21+ и Docker:

```bash
docker compose up -d
docker compose exec -T postgres pg_isready -U student -d orders
# Дождитесь accepting connections, затем выполните схему явно:
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U student -d orders < sql/create-orders.sql
mvn clean test
mvn spring-boot:run -Dspring-boot.run.arguments=--app.demo.data-experiment=repository-basic
```

Заменяйте значение свойства на одно из значений таблицы ниже. Можно записать его
в `application.properties`. Пустое или неизвестное значение печатает список без
подготовки и без записи в БД; подключение и schema validation всё равно выполняются.

БД изолирована от предыдущего урока: отдельный Compose project/volume, порт 5433,
пользователь/пароль student, БД orders. Можно переопределить `DB_URL`, `DB_USER`,
`DB_PASSWORD`. `ddl-auto=validate`, `spring.sql.init.mode=never`: Hibernate не создаёт
и не меняет таблицы. SQL подходит также для явного расширения старой схемы,
но по умолчанию проект работает с отдельной БД.

Каждый опыт самостоятельно восстанавливает пять заказов 6001–6005, покупатели
6201–6202, товар 6101 и позиции 6301–6305. Заказ 6006 и товар 6102 зарезервированы
для записи. Подготовка удаляет/создаёт только эти учебные данные отдельной
подтверждённой транзакцией. Не используйте эти id для других данных и не запускайте
тесты/эксперименты параллельно на одной БД. Остальные строки не удаляются;
findAll/count показывают также их, если вы добавляли свои данные.

`=== PREPARE ===` и последующие проверки БД дают собственный SQL. При подсчёте
SQL основного опыта смотрите только между его маркерами. `PASS` означает, что
проверки результата прошли; неожиданное поведение завершает приложение с ошибкой.

Для последовательной проверки всех опытов отдельными процессами:

```bash
bash scripts/verify-experiments.sh
```

Логи каждого запуска: `/tmp/order-spring-data-jpa.<experiment>.log`, нужен `rg`.
JUnit tests тоже требуют подготовленную настоящую PostgreSQL, без автоматического
пропуска при недоступной БД. Никаких H2, Testcontainers или автогенерации схемы нет.

## 19. Какой SQL ожидать

Без SQL подготовки и проверочных чтений:

| Значение `app.demo.data-experiment` | SQL / наблюдение |
|---|---|
| `repository-basic` | SELECT по id, список, count, exists |
| `repository-save` | INSERT через persist; SELECT + UPDATE через merge |
| `save-assigned-id` | Product: SELECT + INSERT; Order: INSERT без предварительного SELECT |
| `derived-query` | SELECT с customer_id и SELECT с total > ? |
| `jpql-query` | SELECT с customer_id = ? |
| `fetch-basic` | SELECT Order, затем items, затем Customer при обращениях |
| `lazy-outside-context` | SELECT Order; вне context исключение, SELECT items невозможен |
| `n-plus-one` | SELECT списка + 5 SELECT items |
| `join-fetch` | обычный 1+5; новый context и один SELECT LEFT JOIN |
| `entity-graph` | SELECT LEFT JOIN, без SELECT при чтении items |
| `transactional-service` | чтения, INSERT второго заказа, UPDATE первого; commit |
| `transactional-rollback` | чтения, INSERT и UPDATE при flush; rollback отменяет оба |
| `dto-projection` | SELECT id, customer.name, total через JOIN |
| `optimistic-locking` | два SELECT, два UPDATE с version в WHERE, второй конфликтует |

Точное форматирование и состав колонок определяет Hibernate. Begin/commit/rollback
не обязаны отображаться как SQL Hibernate; границы видны в коде и маркерах.
Включены SQL, форматирование и bind parameters, без DEBUG всего Spring.

Официальные источники:
[сохранение и определение новой Entity](https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html),
[запросы и EntityGraph](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html),
[транзакции Spring Data](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html),
[Jakarta Persistence](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2).


## Проверка реализации

Проверено 22 октября 2026 года на PostgreSQL 18 (Docker), Spring Boot 4.1.1,
Hibernate 7.4.5.Final. `mvn clean test`: **9 tests, 0 failures, 0 errors, BUILD SUCCESS**.
Все 14 опытов запущены отдельными процессами и завершились PASS. Фактические SQL
соответствуют таблице выше; N+1: 6 SELECT, JOIN FETCH: 1 SELECT, EntityGraph: 1 SELECT.
Неизвестное значение проверено отдельно: список опытов, без DML.
Подробнее: [отчёт о проверке](VERIFICATION.md).

Сборка и запуски выполнены на установленной JVM 25 с `--release 21`.
Отдельный запуск именно на JVM 21 не проверен. Тесты требуют доступной БД;
при её отсутствии они завершаются ошибкой, а не скрывают непроверенные механизмы.
