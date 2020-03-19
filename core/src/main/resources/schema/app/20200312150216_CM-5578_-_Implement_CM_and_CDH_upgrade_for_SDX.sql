-- // CM-5578 - Implement CM and CDH upgrade for SDX
-- Migration SQL that makes the change goes here.
CREATE SEQUENCE IF NOT EXISTS revision_id_seq  INCREMENT 1  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;

CREATE TABLE IF NOT EXISTS revision_info (
	rev int4 NOT NULL,
	"timestamp" int8 NULL,
	CONSTRAINT revinfo_pkey PRIMARY KEY (rev)
);

CREATE TABLE IF NOT EXISTS component_history (
	id int8 NOT NULL,
	rev int4 NOT NULL,
	revtype int2 NULL,
	"attributes" text NULL,
	componenttype varchar(255) NULL,
	"name" varchar(255) NULL,
	stack_id int8 NULL,
	CONSTRAINT component_history_pkey PRIMARY KEY (id,rev)
);

CREATE TABLE IF NOT EXISTS clustercomponent_history (
	id int8 NOT NULL,
	rev int4 NOT NULL,
	revtype int2 NULL,
	"attributes" text NULL,
	componenttype varchar(255) NULL,
	"name" varchar(255) NULL,
	cluster_id int8 NULL,
	CONSTRAINT clustercomponent_history_pkey PRIMARY KEY (id,rev)
);

ALTER TABLE component_history ADD CONSTRAINT fk_component_revinfo_rev FOREIGN KEY (rev) REFERENCES revision_info(rev);
ALTER TABLE clustercomponent_history ADD CONSTRAINT fk_clcomponent_revinfo_rev FOREIGN KEY (rev) REFERENCES revision_info(rev);

ALTER TABLE revision_info
   ALTER COLUMN rev SET DEFAULT nextval ('revision_id_seq');



-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS component_history CASCADE;
DROP TABLE IF EXISTS clustercomponent_history CASCADE;
DROP TABLE IF EXISTS revision_info;
DROP SEQUENCE IF EXISTS revision_id_seq;


