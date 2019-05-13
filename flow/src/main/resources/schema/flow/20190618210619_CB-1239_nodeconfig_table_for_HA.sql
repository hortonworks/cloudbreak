-- // CB-1239 nodeconfig table for HA
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS node (
	uuid varchar(255) NOT NULL,
	lastupdated int8 NOT NULL,
	"version" int8 NOT NULL,
	CONSTRAINT node_pkey PRIMARY KEY (uuid)
);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS node;
