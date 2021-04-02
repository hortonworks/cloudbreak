-- // CB-11904 Custom image management
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE IF NOT EXISTS customimagecatalog_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

CREATE TABLE IF NOT EXISTS customimagecatalog (
    id bigint PRIMARY KEY DEFAULT nextval('customimagecatalog_id_seq'),
    resourcecrn VARCHAR(255),
    creator VARCHAR(255),
    created int8 NOT NULL DEFAULT (date_part('epoch'::text, now()) * 1000::double precision),
    account character varying(255) NOT NULL,
    owner character varying(255) NOT NULL,
    workspace_id int8 NOT NULL,
    name character varying(255) NOT NULL,
    description TEXT
);

ALTER TABLE customimagecatalog
    ADD CONSTRAINT customimagecatalogname_in_workspace_unique UNIQUE (name, workspace_id),
    ADD CONSTRAINT fk_customimagecatalog_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id);

CREATE SEQUENCE IF NOT EXISTS customimage_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

CREATE TABLE IF NOT EXISTS customimage (
    id bigint PRIMARY KEY DEFAULT nextval('customimage_id_seq'),
    customimagecatalog_id bigint NOT NULL,
    resourcecrn VARCHAR(255),
    creator VARCHAR(255),
    created int8 NOT NULL DEFAULT (date_part('epoch'::text, now()) * 1000::double precision),
    name character varying(255) NOT NULL,
    description TEXT,
    customizedimageid character varying(255) NOT NULL,
    imageType VARCHAR(31),
    baseparcelurl character varying(255)
);

ALTER TABLE customimage
    ADD CONSTRAINT customimagename_in_customimagecatalog_unique UNIQUE (name, customimagecatalog_id),
    ADD CONSTRAINT fk_customimage_customimagecatalog FOREIGN KEY (customimagecatalog_id) REFERENCES customimagecatalog(id);

CREATE SEQUENCE IF NOT EXISTS vmimage_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

CREATE TABLE IF NOT EXISTS vmimage (
    id bigint PRIMARY KEY DEFAULT nextval('vmimage_id_seq'),
    customimage_id bigint NOT NULL,
    creator VARCHAR(255),
    created int8 NOT NULL DEFAULT (date_part('epoch'::text, now()) * 1000::double precision),
    region character varying(255) NOT NULL,
    imagereference character varying(255) NOT NULL
);

ALTER TABLE vmimage
    ADD CONSTRAINT vmimageregion_in_customimage_unique UNIQUE (region, customimage_id),
    ADD CONSTRAINT fk_vmimage_customimage FOREIGN KEY (customimage_id) REFERENCES customimage(id);

-- //@UNDO
-- SQL to undo the change goes here.
DROP TABLE IF EXISTS vmimage;
DROP SEQUENCE IF EXISTS vmimage_id_seq;
DROP TABLE IF EXISTS customimage;
DROP SEQUENCE IF EXISTS customimage_id_seq;
DROP TABLE IF EXISTS customimagecatalog;
DROP SEQUENCE IF EXISTS customimagecatalog_id_seq;