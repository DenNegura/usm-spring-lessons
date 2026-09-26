# Transactions, Persistence Context и жизненный цикл Entity

Следующая лекция после `order-jdbc-hibernate`. Проект создан как его копия:
сохранены Spring Boot 4.1.1, PostgreSQL, assigned id, пакеты (включая `entitie`),
доменный `Order`, `OrderEntity`, JDBC/JPA/console repositories и уведомления.
Java target — 21. `OrderService → OrderRepository` по-прежнему реализует DIP:
бизнес-сервис ничего не знает о Hibernate. Все эксперименты находятся в `demo`.
Spring Data repository interfaces и `@Transactional` не используются.

## Запуск

Команды из каталога `order-persistence-context`, требуются Maven, JDK 21 и Docker:

```bash
docker compose up -d
docker compose ps
mvn clean test
mvn spring-boot:run
mvn spring-boot:run -Dspring-boot.run.arguments=--app.demo.persistence-experiment=dirty-checking
```

Compose использует имя проекта `order-jdbc-hibernate`, прежние `orders-postgres`,
volume и порт 5432. Это та же учебная БД. Не запускайте эксперименты параллельно.
Если предыдущий проект запускали с другим `-p`, используйте такое же значение
и здесь. `docker compose down -v` удаляет данные — для этой лекции не нужен.

Таблица `orders` должна уже существовать после предыдущей лекции. Для новой,
пустой БД предоставлен явный SQL, который преподаватель выполняет вручную:

```bash
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U student -d orders < sql/create-orders.sql
```

`spring.jpa.hibernate.ddl-auto=validate`, `spring.sql.init.mode=never`:
приложение не создаёт и не изменяет schema. Несовместимая schema приводит к ошибке.

Можно записать `app.demo.persistence-experiment=merge` в `application.properties`.
`PersistenceDemoProperties` использует существующий подход `@ConfigurationProperties`.
Runner выбирает ровно один метод через `switch`. Пустое/неизвестное значение выводит
список и ничего не записывает в БД, хотя подключение и schema validation нужны.
Прежний бизнес-runner сохранён и отключён `app.demo.enabled=false`.
Для этой лекции оставьте его отключённым.

## Как читать console

`PREPARE` — отдельная завершённая transaction подготовки. Она восстанавливает
`total=100.00` для id 201 (основные опыты) или 202 (remove).
Batch очищает только зарезервированные id 2201–2223 перед вставкой.
Эти id принадлежат демонстрациям: не используйте их для других заказов.
Остальные строки не затрагиваются. Каждому опыту соответствует раздел ниже.
SQL подготовки не относится к основному опыту. После него `VERIFY FROM NEW CONTEXT`
читает результат новым EntityManager, а `PASS` печатается только после проверок.
При несоответствии runner бросает исключение, процесс завершается с ошибкой.
SQL и bind parameters включены; transaction begin/commit/rollback не обязаны
печататься как SQL Hibernate, их границы обозначены кодом и сообщениями.

| Experiment | Что показывает | SQL основного опыта (без PREPARE и VERIFY) |
|---|---|---|
| `transaction-rollback` | flush отправляет изменения, rollback отменяет их | SELECT + UPDATE при flush; в новом context total снова 100.00 |
| `identity-cache` | persistent identity и L1 cache | SELECT первого find; второй без SELECT; SELECT после clear |
| `dirty-checking` | изменение managed object без save/update/merge | SELECT + UPDATE при commit; итог 999.00 |
| `detach` | локальное изменение detached object не отслеживается | SELECT, без UPDATE после detach; итог 100.00 |
| `merge` | копирование detached state в другой managed instance | SELECT в первом context; обычно SELECT при merge во втором, UPDATE при commit; итог 777.00 |
| `remove` | REMOVED и запланированный DELETE | SELECT + DELETE; новый find возвращает null |
| `batch` | NEW → MANAGED → DETACHED, flush перед clear | 23 INSERT в группах 5/5/5/5/3; один общий commit |

Для последовательной проверки всех опытов отдельными запусками (не для показа лекции):

```bash
bash scripts/verify-experiments.sh
```

Скрипт сохраняет отдельные логи в `/tmp/order-persistence-context.*` и требует `rg`.

## Transaction и Persistence Context

Transaction отвечает: «Будут ли изменения БД подтверждены или откатаны?»
Persistence Context отвечает: «Какими Entity Hibernate сейчас управляет?»
В методах видна последовательность `create EntityManager → begin → работа →
commit/rollback → close`. Это разные механизмы, хотя границы здесь близки.
В нашем application-managed EntityManager после commit объект ещё managed:
это проверяется через contains в identity-cache и merge. Только затем закрываем context.

`flush()` синхронизирует изменения с БД внутри текущей transaction, но не подтверждает
её. Rollback отменяет даже уже выполненный UPDATE. Он не обязан восстанавливать
поля Java object. Поэтому результат rollback читаем через новый EntityManager.

Persistence Context обеспечивает Identity Map и First-Level Cache, а также tracking
и lifecycle: это не только кеш. Одна persistent identity (тип Entity + primary key)
соответствует одному managed instance в context. `first == second` — true,
после clear `first == third` — false. identityHashCode только иллюстрирует ссылки;
строгая проверка выполняется через `==`.
L1 cache принадлежит context и существует автоматически. Это не Hibernate L2,
не Spring Cache и не Redis. Query cache и сторонние кеши не включены.

## Lifecycle и dirty checking

* NEW / TRANSIENT: созданный объект ещё не managed, даже если assigned id уже задан.
* MANAGED: после persist/find текущий context управляет объектом, contains=true.
* DETACHED: объект существует в Java, но context больше им не управляет.
* REMOVED: объект запланирован на удаление. Hibernate сохраняет информацию для DELETE;
  это не обычный detached object. После remove `contains` уже false по контракту JPA.

`contains=false` само по себе не различает NEW, DETACHED и REMOVED. Мы знаем state
из выполненных операций, собственного production state detector здесь нет.
Batch показывает NEW/ persist / MANAGED / clear / DETACHED; remove — REMOVED.

Центральный метод `updateUsingManagedEntity`: `find → changeTotal → commit`.
Где UPDATE? Hibernate обнаруживает изменение managed Entity при dirty checking
и синхронизирует его при flush/commit. Никакого дополнительного merge не нужно.

## detach, clear, close

`detach(entity)` отсоединяет одну Entity. `clear()` отсоединяет все Entity context.
`close()` завершает EntityManager; оставшиеся managed objects становятся detached.
На закрытом EntityManager нельзя вызывать contains (IllegalStateException):
в merge показываем contains до close и contains старого объекта в новом context.

Намеренный detach полезен для локальной обработки объекта без сохранения дальнейших
изменений, в длинной unit of work, когда объект больше не нужен managed, или для
специальной инфраструктурной логики tracking. В обычном коротком Spring service
method он нужен редко. Не вызывайте detach после каждого find, как оптимизацию
по умолчанию, вместо DTO или rollback либо пока Entity нужна текущей unit of work.

## Что именно делает merge

`merge(detached)` получает managed representation в текущем context, копирует
в него state аргумента и возвращает managed instance. Исходный объект остаётся
detached. В опыте два разных context, поэтому возвращается другой Java instance.
После merge продолжать работу нужно с возвращённым объектом:

```java
OrderEntity managed = em.merge(detached);
detached.changeTotal(new BigDecimal("555.00")); // ошибка: меняется НЕ managed object
```

Правильно:

```java
OrderEntity managed = em.merge(detached);
managed.changeTotal(new BigDecimal("777.00"));
```

В runnable опыте выполнены оба изменения, чтобы новый context подтвердил 777.00.
Merge — не команда UPDATE и не универсальный upsert «есть строка → UPDATE,
нет строки → INSERT». После копирования state Hibernate определяет синхронизацию.
Для detached объекта вне context часто нужен SELECT; если managed representation
уже есть, дополнительный SELECT может не понадобиться. Для NEW merge разрешён
и может привести к INSERT, но для явно нового объекта понятнее persist.
Assigned id здесь не является универсальным признаком NEW или DETACHED;
поведение при отсутствующей строке зависит также от mapping/provider/version.

Когда имеются только id и новое total, обычно выбираем `find + changeTotal`.
Когда состояние действительно пришло из завершённого context и его нужно перенести,
уместен merge. Он копирует состояние Entity, поэтому для частичного изменения
не стоит искусственно собирать объект с незаполненными остальными полями.

## Batch: почему flush перед clear

Длинный цикл persist удерживает managed Entity в context. Каждые пять объектов
flush отправляет SQL, затем clear освобождает context от обработанных Entity.
Clear не гарантирует освобождение памяти, если другие части Java-кода держат ссылки.
Если сначала вызвать clear, несинхронизированное состояние может быть потеряно.
Последние три Entity тоже явно flush/clear. Новый context проверяет все 23 строки
и их total. Flush не делит transaction: rollback отменил бы всю пачку.
Число 5 — размер порции очистки context, не настройка JDBC batching;
JDBC batch_size отдельно не включён, в логе видны индивидуальные INSERT.

## Источник и проверка

Семантика методов сверена с [официальным API Jakarta Persistence EntityManager](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/entitymanager).
Конкретный состав колонок UPDATE и формат SQL определяет Hibernate; изменение
одного total не означает, что UPDATE обязан содержать только эту колонку.

Проверка 2026-09-25: `mvn clean test` — BUILD SUCCESS. В исходном проекте нет
JUnit-тестов; здесь проверка поведения реализована runnable assertions и скриптом
семи интеграционных запусков на настоящем PostgreSQL. Все семь завершились PASS.
Проверены фактические SELECT/INSERT/UPDATE/DELETE, identity, contains, rollback,
отсутствие UPDATE после detach, значение managed-копии merge и все 23 batch-строки.
Hibernate при merge выполнил SELECT во втором context и UPDATE при commit.
Сборка и запуски выполнены установленной JVM 25 с компиляцией `--release 21`;
отдельной JVM 21 в окружении нет, запуск именно на ней не проверен.
