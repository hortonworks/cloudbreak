-- // create_filesystem
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS constrainttemplate
(
    id                  BIGINT NOT NULL,
    name                CHARACTER VARYING (255) NOT NULL,
    account             CHARACTER VARYING(255),
    deleted             BOOLEAN NOT NULL,
    description         TEXT,
    owner               CHARACTER VARYING(255),
    publicinaccount     BOOLEAN NOT NULL,
    cpu                 FLOAT(32) NOT NULL,
    memory              FLOAT(32) NOT NULL,
    disk                FLOAT(32) NOT NULL,
    status              CHARACTER VARYING (255) DEFAULT('CREATED')
);

CREATE SEQUENCE constrainttemplate_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER TABLE ONLY constrainttemplate
    ADD CONSTRAINT constrainttemplate_pkey PRIMARY KEY (id);

ALTER TABLE ONLY constrainttemplate
    ADD CONSTRAINT uk_constrainttemplate_account_name UNIQUE (account, name);

CREATE TABLE IF NOT EXISTS hostgroup_constraint
(
    id                      BIGINT NOT NULL,
    instancegroup_id        BIGINT,
    constrainttemplate_id   BIGINT,
    hostcount               INTEGER NOT NULL
);

CREATE SEQUENCE hostgroup_constraint_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER TABLE ONLY hostgroup_constraint
    ADD CONSTRAINT hostgroup_constraint_pkey PRIMARY KEY (id);

ALTER TABLE ONLY hostgroup_constraint
    ADD CONSTRAINT fk_hostgroup_constraint_instancegroup_id FOREIGN KEY (instancegroup_id) REFERENCES instancegroup(id);

ALTER TABLE ONLY hostgroup_constraint
    ADD CONSTRAINT fk_hostgroup_constraint_constrainttemplate_id FOREIGN KEY (constrainttemplate_id) REFERENCES constrainttemplate(id);

ALTER TABLE ONLY hostgroup
    ADD COLUMN constraint_id BIGINT;

ALTER TABLE ONLY hostgroup
    ADD CONSTRAINT fk_hostgroup_hostgroup_constraint_id FOREIGN KEY (constraint_id) REFERENCES hostgroup_constraint(id);

INSERT INTO hostgroup_constraint(
	id,
	instancegroup_id,
	hostcount
)SELECT
	nextval('hostgroup_constraint_id_seq') AS "id",
	ig.id AS "instancegroup_id",
	ig.nodecount AS "hostcount"
FROM cluster c, hostgroup h, instancegroup ig
WHERE h.cluster_id = c.id AND ig.id = h.instancegroup_id
GROUP BY ig.nodecount,  ig.id,  ig.groupname;


WITH constraint_hostgroup_temp AS (
	SELECT
		hc.id AS "constraint_id",
		h.id AS "hostgroup_id"
	FROM hostgroup_constraint hc, hostgroup h
	WHERE h.instancegroup_id = hc.instancegroup_id
	GROUP BY h.id, hc.id
) UPDATE hostgroup SET constraint_id=cht.constraint_id FROM constraint_hostgroup_temp cht WHERE cht.hostgroup_id=id;

ALTER TABLE ONLY hostgroup
    DROP COLUMN instancegroup_id;


-- //@UNDO
-- SQL to undo the change goes here.

DROP SEQUENCE IF EXISTS constrainttemplate_id_seq;
DROP SEQUENCE IF EXISTS hostgroup_constraint_id_seq;

ALTER TABLE ONLY hostgroup
    ADD COLUMN instancegroup_id BIGINT;

ALTER TABLE ONLY hostgroup
    ADD CONSTRAINT fk_hostgroup_instancegroup_id FOREIGN KEY (instancegroup_id) REFERENCES instancegroup(id);

UPDATE hostgroup SET instancegroup_id=hc.instancegroup_id FROM hostgroup_constraint hc WHERE constraint_id=hc.id;

ALTER TABLE ONLY hostgroup
    DROP COLUMN constraint_id;


DROP TABLE IF EXISTS hostgroup_constraint;
DROP TABLE IF EXISTS constrainttemplate;