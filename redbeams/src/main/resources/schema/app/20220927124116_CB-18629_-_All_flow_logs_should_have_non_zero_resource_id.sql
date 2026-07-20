-- // CB-18629 - All flow logs should have non zero resource id
-- Migration SQL that makes the change goes here.

DO $$
BEGIN
    -- Check whether the 'flowlog' table exists in the current schema
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'flowlog'
    ) THEN
        UPDATE flowlog f
        SET resourceid =
            (
            SELECT MAX(resourceid)
            FROM flowlog
            WHERE f.flowid = flowid
            )
        WHERE resourceid = 0;
    END IF;
END $$;

-- //@UNDO
-- SQL to undo the change goes here.
-- Left blank, no point in rollback
