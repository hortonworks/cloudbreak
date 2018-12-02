-- // datalake resources related new entities
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS datalakeresources (
    id bigserial NOT NULL,
    datalakestack_id bigserial,
    name character varying(255) NOT NULL,
    datalakeambariip character varying(255) NOT NULL,
    datalakeambarifqdn character varying(255) NOT NULL,
    workspace_id bigint NOT NULL,
    datalakecomponents TEXT NOT NULL
);
ALTER TABLE ONLY datalakeresources ADD CONSTRAINT datalakeresources_pkey PRIMARY KEY (id);
ALTER TABLE ONLY datalakeresources ADD CONSTRAINT fk_stack_datalakestack_id FOREIGN KEY (datalakestack_id) REFERENCES stack(id);
ALTER TABLE ONLY datalakeresources ADD CONSTRAINT uk_datalakeresources_workspace_name UNIQUE (workspace_id, name);
CREATE INDEX IF NOT EXISTS idx_datalakeresources_workspace_id_name ON datalakeresources (workspace_id, name);
CREATE INDEX IF NOT EXISTS idx_datalakeresources_datalakestack_id ON datalakeresources (datalakestack_id);

CREATE TABLE IF NOT EXISTS servicedescriptor (
    id bigserial NOT NULL,
    datalakeresources_id bigint NOT NULL,
    workspace_id bigint NOT NULL,
    servicename character varying(255) NOT NULL,
    blueprintparams TEXT NOT NULL,
    blueprintsecretparams TEXT,
    componentshosts TEXT NOT NULL
);
ALTER TABLE ONLY servicedescriptor ADD CONSTRAINT servicedescriptor_pkey PRIMARY KEY (id);
ALTER TABLE ONLY servicedescriptor ADD CONSTRAINT fk_servicedescriptor_datalakeresources_id FOREIGN KEY (datalakeresources_id) REFERENCES datalakeresources(id);
CREATE INDEX IF NOT EXISTS idx_servicedescriptor_datalakeresources_id ON servicedescriptor (datalakeresources_id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_servicedescriptor_datalakeresources_id;
ALTER TABLE ONLY servicedescriptor DROP CONSTRAINT IF EXISTS fk_servicedescriptor_datalakeresources_id;
ALTER TABLE ONLY servicedescriptor DROP CONSTRAINT IF EXISTS servicedescriptor_pkey;
DROP TABLE IF EXISTS servicedescriptor;

DROP INDEX IF EXISTS idx_datalakeresources_datalakestack_id;
DROP INDEX IF EXISTS idx_datalakeresources_workspace_id_name;
ALTER TABLE ONLY datalakeresources DROP CONSTRAINT IF EXISTS uk_datalakeresources_workspace_name;
ALTER TABLE ONLY datalakeresources DROP CONSTRAINT IF EXISTS fk_stack_datalakestack_id;
ALTER TABLE ONLY datalakeresources DROP CONSTRAINT IF EXISTS datalakeresources_pkey;
DROP TABLE IF EXISTS datalakeresources;
