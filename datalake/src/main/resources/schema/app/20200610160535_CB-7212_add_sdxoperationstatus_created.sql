-- // CB-7212 Add datalake service changes to perform database backup/restore.
-- Migration SQL that makes the change goes here.

create table if not exists sdxoperation
(
    id bigserial NOT NULL,
	operationtype character varying(255) NOT NULL,
	sdxclusterid bigint not null,
	operationid character varying(255) NOT NULL,
	statusreason character varying(255),
	status character varying(50) NOT NULL,
	PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS unq_index_sdxoperation_operationId ON sdxoperation (operationid);


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS unq_index_sdxoperation_operationId;
DROP TABLE IF EXISTS sdxoperation;
