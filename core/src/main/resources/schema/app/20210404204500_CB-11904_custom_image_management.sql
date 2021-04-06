-- // CB-11904 Custom image management
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE IF NOT EXISTS customimage_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

CREATE TABLE IF NOT EXISTS customimage (
    id bigint PRIMARY KEY DEFAULT nextval('customimage_id_seq'),
    imagecatalog_id bigint NOT NULL,
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
    ADD CONSTRAINT customimagename_in_imagecatalog_unique UNIQUE (name, imagecatalog_id),
    ADD CONSTRAINT customimageresourcecrn_unique UNIQUE (resourcecrn),
    ADD CONSTRAINT fk_customimage_imagecatalog FOREIGN KEY (imagecatalog_id) REFERENCES imagecatalog(id);

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