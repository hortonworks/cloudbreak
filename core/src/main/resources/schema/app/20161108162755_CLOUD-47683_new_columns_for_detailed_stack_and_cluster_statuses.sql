-- // CLOUD-47683 new columns for detailed stack and cluster statuses
-- Migration SQL that makes the change goes here.

ALTER TABLE cloudbreakevent ADD COLUMN statustype varchar(255);
ALTER TABLE cloudbreakevent ADD COLUMN detailedStackStatus varchar(255);

CREATE SEQUENCE stackstatus_id_seq START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE stackstatus (
    id BIGINT NOT NULL,
    created BIGINT NOT NULL DEFAULT (date_part('epoch'::text, now()) * (1000)::double precision),
    stack_id bigint,
    status character varying(255),
    detailedstackstatus character varying(255),
    statusreason text
);

ALTER TABLE stackstatus
    ADD CONSTRAINT PK_stackstatus PRIMARY KEY (id),
    ALTER COLUMN id SET DEFAULT nextval ('stackstatus_id_seq');

ALTER TABLE stack ADD COLUMN stackstatus_id bigint;

INSERT INTO stackstatus (status, statusreason, stack_id, created, detailedstackstatus) SELECT s.status, s.statusreason, s.id, s.created, 'UNKNOWN' as  detailedstackstatus FROM stack s;
UPDATE stack SET stackstatus_id = stackstatus.id FROM stackstatus WHERE stack.id = stackstatus.stack_id;

ALTER TABLE stack
   DROP COLUMN status,
   DROP COLUMN statusreason;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack
    ADD COLUMN status character varying(255),
    ADD COLUMN statusreason text;

UPDATE stack SET (status, statusreason) = (stackstatus.status, stackstatus.statusreason) FROM stackstatus WHERE stack.id = stackstatus.stack_id;

ALTER TABLE stack DROP COLUMN stackstatus_id;

DROP TABLE stackstatus;
DROP SEQUENCE stackstatus_id_seq;

ALTER TABLE cloudbreakevent DROP COLUMN statustype;
ALTER TABLE cloudbreakevent DROP COLUMN detailedStackStatus;
