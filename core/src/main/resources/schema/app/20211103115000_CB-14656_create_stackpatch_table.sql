-- // CB-14656
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS stackpatch
(
    id              bigint NOT NULL,
    type            CHARACTER VARYING(255) NOT NULL,
    created         int8 NOT NULL,
    stack_id        bigint NOT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE ONLY stackpatch ADD CONSTRAINT fk_stackpatch_stack_id FOREIGN KEY (stack_id) REFERENCES stack(id);

ALTER TABLE ONLY stackpatch ADD CONSTRAINT uk_stackpatch_stack_type UNIQUE (stack_id, type);

CREATE INDEX IF NOT EXISTS stackpatch_type_stack ON stackpatch(stack_id, type);

CREATE SEQUENCE IF NOT EXISTS stackpatch_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS stackpatch;

DROP SEQUENCE IF EXISTS stackpatch_id_seq;
