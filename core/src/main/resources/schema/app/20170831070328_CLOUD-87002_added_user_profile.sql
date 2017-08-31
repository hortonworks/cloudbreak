-- // CLOUD-87002 added user profile
-- Migration SQL that makes the change goes here.


CREATE TABLE IF NOT EXISTS userprofile
(
    id              bigint NOT NULL,
    owner           CHARACTER VARYING(255) NOT NULL,
    account         CHARACTER VARYING (255) NOT NULL,
    credential_id   bigint
);

ALTER TABLE ONLY userprofile ADD CONSTRAINT fk_userprofile_credential_id FOREIGN KEY (credential_id) REFERENCES credential(id);

CREATE SEQUENCE userprofile_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- //@UNDO
-- SQL to undo the change goes here.

DROP SEQUENCE userprofile_id_seq;

DROP TABLE IF EXISTS userprofile;