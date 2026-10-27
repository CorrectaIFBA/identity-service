-- Runs before Hibernate on every startup. Idempotent: converts legacy ordinal enum columns to strings.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'institutional_profiles'
          AND column_name = 'type'
          AND data_type IN ('smallint', 'integer')
    ) THEN
        ALTER TABLE institutional_profiles DROP CONSTRAINT IF EXISTS institutional_profiles_type_check;
        ALTER TABLE institutional_profiles ALTER COLUMN type TYPE varchar(20) USING (
            CASE type WHEN 0 THEN 'FACULTY' WHEN 1 THEN 'STUDENT' WHEN 2 THEN 'STAFF' END
        );
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'institutional_profiles'
          AND column_name = 'state'
          AND data_type IN ('smallint', 'integer')
    ) THEN
        ALTER TABLE institutional_profiles DROP CONSTRAINT IF EXISTS institutional_profiles_state_check;
        ALTER TABLE institutional_profiles ALTER COLUMN state TYPE varchar(20) USING (
            CASE state WHEN 0 THEN 'PENDING' WHEN 1 THEN 'ACTIVE' WHEN 2 THEN 'INACTIVE' END
        );
    END IF;
END $$;
^^^
