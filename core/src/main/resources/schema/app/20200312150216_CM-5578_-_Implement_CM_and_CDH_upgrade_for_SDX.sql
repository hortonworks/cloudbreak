-- // CM-5578 - Implement CM and CDH upgrade for SDX
-- Migration SQL that makes the change goes here.
CREATE TABLE IF NOT EXISTS revinfo (
	rev int4 NOT NULL,
	revtstmp int8 NULL,
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

ALTER TABLE component_history ADD CONSTRAINT fk_revinfo_rev FOREIGN KEY (rev) REFERENCES revinfo(rev);



-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS component_history;
DROP TABLE IF EXISTS revinfo;


