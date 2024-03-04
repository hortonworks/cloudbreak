-- // CB-25017 adding base scheme for private control planes
-- Migration SQL that makes the change goes here.


CREATE TABLE private_control_plane
(
    id                      bigserial NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    resourcecrn             VARCHAR (255) NOT NULL,
    accountid               VARCHAR (255) NOT NULL,
    CONSTRAINT private_control_plane_pkey PRIMARY KEY (id)
);


CREATE SEQUENCE IF NOT EXISTS private_control_plane_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

-- //@UNDO
-- SQL to undo the change goes here.

DROP SEQUENCE IF EXISTS private_control_plane_id_seq;

ALTER TABLE ONLY private_control_plane DROP CONSTRAINT IF EXISTS private_control_plane_pkey;

DROP TABLE IF EXISTS private_control_plane;
