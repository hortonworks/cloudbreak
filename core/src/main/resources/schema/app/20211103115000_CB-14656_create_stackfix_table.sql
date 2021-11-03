-- // CB-14656
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS stackfix
(
    id              bigint NOT NULL,
    type            CHARACTER VARYING(255) NOT NULL,
    created         int8 NOT NULL,
    stack_id        bigint NOT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE ONLY stackfix ADD CONSTRAINT fk_stackfix_stack_id FOREIGN KEY (stack_id) REFERENCES stack(id);

CREATE SEQUENCE IF NOT EXISTS stackfix_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS stackfix;

DROP SEQUENCE IF EXISTS stackfix_id_seq;
