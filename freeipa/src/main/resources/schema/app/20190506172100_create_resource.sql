-- // Create Changelog

-- Default DDL for changelog table that will keep
-- a record of the migrations that have been run.

-- You can modify this to suit your database before
-- running your first migration.

-- Be sure that ID and DESCRIPTION fields exist in
-- BigInteger and String compatible fields respectively.

CREATE SEQUENCE resource_id_seq START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS resource
(
    id BIGINT DEFAULT nextval('resource_id_seq'::regclass) NOT NULL
        CONSTRAINT resource_pkey
            PRIMARY KEY,
    instancegroup VARCHAR(255),
    resourcename VARCHAR(255),
    resourcetype VARCHAR(255) NOT NULL,
    resource_stack BIGINT
        CONSTRAINT fk_resource_resource_stack
            REFERENCES stack,
    resourcereference VARCHAR(255),
    resourcestatus VARCHAR(255) DEFAULT 'CREATED'::CHARACTER VARYING NOT NULL,
    instanceid VARCHAR(255),
    attributes text,
    CONSTRAINT uk_namebytypebystack
        UNIQUE (resourcename, resourcetype, resource_stack)
);

-- //@UNDO

DROP TABLE IF EXISTS resource;

DROP SEQUENCE IF EXISTS resource_id_seq;