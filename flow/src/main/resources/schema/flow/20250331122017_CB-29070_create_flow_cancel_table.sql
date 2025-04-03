-- // CB-29070 create flow cancel table
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE flowcancel_id_seq START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE flowcancel (
    id BIGINT NOT NULL,
    created BIGINT NOT NULL DEFAULT (date_part('epoch'::text, now()) * (1000)::double precision),
    resourceid BIGINT NOT NULL
);

ALTER TABLE flowcancel
    ADD CONSTRAINT PK_flowcancel PRIMARY KEY (id),
    ALTER COLUMN id SET DEFAULT nextval ('flowcancel_id_seq');

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE flowcancel;

DROP SEQUENCE flowcancel_id_seq;