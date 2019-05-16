-- // add more volumes
-- Migration SQL that makes the change goes here.

ALTER TABLE IF EXISTS template DROP COLUMN volumeSize, DROP COLUMN volumeCount, DROP COLUMN volumeType;

CREATE TABLE volumetemplate (
    id bigint NOT NULL,
    volumecount integer,
    volumesize integer,
    volumetype character varying(255),
    template_id bigint
);

CREATE SEQUENCE volume_template_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE volumetemplate
   ALTER COLUMN id SET DEFAULT nextval ('volume_template_id_seq');

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS volumetemplate;

DROP SEQUENCE IF EXISTS volume_template_id_seq;

ALTER TABLE IF EXISTS template ADD COLUMN volumeSize integer, ADD COLUMN volumeCount integer, ADD COLUMN volumeType character varying(255);