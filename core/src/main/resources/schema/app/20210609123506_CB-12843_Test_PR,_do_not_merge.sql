-- // CB-12843 Test PR, do not merge
-- Migration SQL that makes the change goes here.

CREATE TABLE testtable (
    id bigint NOT NULL,
    volumecount integer,
    volumesize4 integer,
    volumetype character varying(255),
    template_id bigint
);

--ALTER TABLE ONLY testtable ADD CONSTRAINT testtable_pkey PRIMARY KEY (id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS testtable;

