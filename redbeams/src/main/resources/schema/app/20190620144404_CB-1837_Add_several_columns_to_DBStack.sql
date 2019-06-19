-- // CB-1837 Add several columns to DBStack
-- Migration SQL that makes the change goes here.

ALTER TABLE dbStack
    ADD COLUMN region VARCHAR(255),
    ADD COLUMN availabilityzone VARCHAR(255),
    ADD COLUMN cloudplatform TEXT,
    ADD COLUMN platformvariant TEXT,
    ADD COLUMN template TEXT,
    ADD COLUMN ownercrn TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE dbStack
    DROP COLUMN region,
    DROP COLUMN availabilityzone,
    DROP COLUMN cloudplatform,
    DROP COLUMN platformvariant,
    DROP COLUMN template,
    DROP COLUMN ownercrn;
