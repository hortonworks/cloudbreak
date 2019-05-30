-- // add more volumes
-- Migration SQL that makes the change goes here.

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

INSERT INTO volumetemplate (volumecount, volumesize, volumetype, template_id) SELECT volumecount, volumesize, volumetype, id FROM template;

ALTER TABLE IF EXISTS template DROP COLUMN volumesize, DROP COLUMN volumecount, DROP COLUMN volumetype;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE IF EXISTS template ADD COLUMN volumesize integer, ADD COLUMN volumecount integer, ADD COLUMN volumetype character varying(255);

UPDATE template
SET volumesize=subquery.volumesize, volumecount=subquery.volumecount, volumetype=subquery.volumetype
FROM (SELECT volumecount, volumetype, volumesize, template_id FROM volumetemplate) AS subquery
WHERE id = subquery.template_id;

DROP TABLE IF EXISTS volumetemplate;

DROP SEQUENCE IF EXISTS volume_template_id_seq;