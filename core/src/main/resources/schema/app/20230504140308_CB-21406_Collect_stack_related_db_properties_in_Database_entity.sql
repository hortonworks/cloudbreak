-- // CB-21406 Collect stack related db properties in Database entity
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS database (
    id bigserial NOT NULL,
    stack_id bigserial NOT NULL,
    externaldatabaseavailabilitytype character varying(255),
    externaldatabaseengineversion character varying(255),
    attributes text,
    PRIMARY KEY (id)
);

ALTER TABLE stack ADD COLUMN IF NOT EXISTS database_id bigint;

ALTER TABLE ONLY stack ADD CONSTRAINT fk_databaseidstack FOREIGN KEY (database_id) REFERENCES database(id);

INSERT INTO database (stack_id, externaldatabaseavailabilitytype, externaldatabaseengineversion)
   SELECT stack.id, stack.externaldatabasecreationtype, stack.externaldatabaseengineversion FROM stack;

UPDATE stack
   SET database_id = database.id
   FROM database
   WHERE database.stack_id = stack.id;

ALTER TABLE database DROP COLUMN IF EXISTS stack_id;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ONLY stack DROP CONSTRAINT IF EXISTS fk_databaseidstack;

ALTER TABLE stack DROP COLUMN IF EXISTS database_id;

DROP INDEX IF EXISTS unq_index_database_stack_id;

DROP TABLE IF EXISTS database;
