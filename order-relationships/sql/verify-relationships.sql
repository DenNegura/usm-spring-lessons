SET search_path TO lesson5;
-- Проверка физических ограничений отдельно от Hibernate validate.
SELECT c.conrelid::regclass AS table_name, c.conname, pg_get_constraintdef(c.oid)
FROM pg_constraint c
WHERE c.conrelid IN ('orders'::regclass, 'order_items'::regclass,
                    'customer_profiles'::regclass, 'student_courses'::regclass)
  AND c.contype IN ('f', 'u', 'p')
ORDER BY table_name, c.conname;

DO $$
DECLARE mapping RECORD;
BEGIN
    FOR mapping IN SELECT * FROM (VALUES
        ('orders', 'customer_id', 'customers'),
        ('order_items', 'order_id', 'orders'),
        ('order_items', 'product_id', 'products'),
        ('customer_profiles', 'customer_id', 'customers'),
        ('student_courses', 'student_id', 'students'),
        ('student_courses', 'course_id', 'courses')
    ) AS expected(source_table, source_column, target_table)
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint c
            JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
            JOIN pg_attribute target ON target.attrelid = c.confrelid AND target.attnum = c.confkey[1]
            WHERE c.contype = 'f' AND cardinality(c.conkey) = 1
              AND c.conrelid = mapping.source_table::regclass
              AND c.confrelid = mapping.target_table::regclass
              AND a.attname = mapping.source_column AND target.attname = 'id'
        ) THEN
            RAISE EXCEPTION 'Missing FK %.% -> %.id', mapping.source_table, mapping.source_column, mapping.target_table;
        END IF;
    END LOOP;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
        WHERE c.conrelid = 'customer_profiles'::regclass AND c.contype = 'u'
          AND cardinality(c.conkey) = 1 AND a.attname = 'customer_id'
    ) THEN RAISE EXCEPTION 'Missing UNIQUE customer_profiles.customer_id'; END IF;
    IF (SELECT count(*) FROM information_schema.columns
        WHERE table_schema = 'lesson5' AND table_name = 'customers'
          AND column_name IN ('address_city', 'address_street')) <> 2
    THEN RAISE EXCEPTION 'Missing embedded columns'; END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_attribute
        WHERE attrelid = 'orders'::regclass AND attname = 'customer_id' AND attnotnull
    ) THEN RAISE EXCEPTION 'orders.customer_id must be NOT NULL'; END IF;
    IF to_regclass('lesson5.address') IS NOT NULL OR to_regclass('lesson5.addresses') IS NOT NULL
    THEN RAISE EXCEPTION 'Address must not have its own table'; END IF;
END $$;
