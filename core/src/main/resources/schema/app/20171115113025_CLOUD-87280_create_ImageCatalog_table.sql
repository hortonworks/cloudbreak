-- // CLOUD-87280 create ImageCatalog table
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE IF NOT EXISTS imagecatalog_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

CREATE TABLE IF NOT EXISTS imagecatalog (
    id bigint PRIMARY KEY DEFAULT nextval('imagecatalog_id_seq'),
    account character varying(255) NOT NULL,
    owner character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    url character varying(255) NOT NULL,
    archived boolean NOT NULL DEFAULT false,
    publicinaccount boolean NOT NULL
);

ALTER TABLE ONLY imagecatalog
    ADD CONSTRAINT uk_imagecatalog_account_name UNIQUE (account, name);

ALTER TABLE ONLY userprofile ADD COLUMN imagecatalog_id bigint;

ALTER TABLE ONLY userprofile ADD CONSTRAINT fk_userprofile_imagecatalog_id FOREIGN KEY (imagecatalog_id) REFERENCES imagecatalog(id);

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE userprofile DROP CONSTRAINT IF EXISTS fk_userprofile_imagecatalog_id;
ALTER TABLE userprofile DROP COLUMN imagecatalog_id;
DROP TABLE IF EXISTS imagecatalog;
DROP SEQUENCE IF EXISTS imagecatalog_id_seq;
