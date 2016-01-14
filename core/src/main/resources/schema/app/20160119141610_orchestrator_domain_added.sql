-- // orchestrator domain added
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS orchestrator
(
    id                  BIGINT NOT NULL,
    apiEndpoint         CHARACTER VARYING (255) NOT NULL,
    type                CHARACTER VARYING (255) NOT NULL,
    attributes          TEXT,
	temp_stackid		BIGINT NOT NULL
);

CREATE SEQUENCE orchestrator_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER TABLE ONLY orchestrator
    ADD CONSTRAINT orchestrator_pkey PRIMARY KEY (id);


ALTER TABLE ONLY stack
    ADD COLUMN orchestrator_id BIGINT;

ALTER TABLE ONLY stack
    ADD CONSTRAINT fk_stack_orchestrator_id FOREIGN KEY (orchestrator_id) REFERENCES orchestrator(id);

INSERT INTO orchestrator(
	id,
	apiEndpoint,
	type,
	temp_stackid
) SELECT
	nextval('orchestrator_id_seq') AS "id",
	concat(im.publicip, ':443') AS "apiEndpoint",
	'SWARM' AS "type",
	s.id AS "temp_stackid"
FROM stack s, instancegroup ig, instancemetadata im
WHERE
	s.status <> 'DELETE_COMPLETED' AND
	ig.stack_id = s.id AND
	ig.id = im.instancegroup_id AND
	im.ambariserver = true;

UPDATE stack SET orchestrator_id=o.id FROM orchestrator o WHERE o.temp_stackid=stack.id;

ALTER TABLE ONLY orchestrator
    DROP COLUMN temp_stackid;

-- //@UNDO
-- SQL to undo the change goes here.

DROP SEQUENCE IF EXISTS orchestrator_id_seq;

ALTER TABLE ONLY stack
    DROP COLUMN orchestrator_id;

DROP TABLE IF EXISTS orchestrator;

