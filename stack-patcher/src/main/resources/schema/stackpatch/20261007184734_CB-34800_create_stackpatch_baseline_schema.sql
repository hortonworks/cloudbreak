-- // CB-34800 create stackpatch baseline schema
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE IF NOT EXISTS stackpatch_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS stackpatch
(
    id              bigint NOT NULL,
    type            CHARACTER VARYING(255) NOT NULL,
    created         int8 NOT NULL,
    resource_id     bigint NOT NULL,
    status          CHARACTER VARYING(255) NOT NULL,
    status_reason   TEXT,
    PRIMARY KEY (id),
    CONSTRAINT uk_stackpatch_resource_type UNIQUE (resource_id, type)
);

-- //@UNDO
-- SQL to undo the change goes here.
-- Intentionally empty: the stackpatch table may predate this baseline in the consuming service's own schema,
-- so dropping it here would delete that service's existing data.
