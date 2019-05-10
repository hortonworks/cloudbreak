-- // CB-1210 create credential table
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS credential (
    id                  bigserial NOT NULL,
    description         text,
    name                varchar(255) NOT NULL,
    publickey           text,
    archived            bool DEFAULT false,
    loginusername       text,
    attributes          text,
    cloudplatform       varchar(255) NOT NULL,
    workspace_id        int8,
    govcloud            bool DEFAULT false,
    CONSTRAINT fk_credential_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id),
    CONSTRAINT credentialname_in_workspace_unique UNIQUE (name, workspace_id),
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS credential_id_idx ON credential USING btree (id);
CREATE INDEX IF NOT EXISTS credential_name_idx ON credential USING btree (name);
CREATE INDEX IF NOT EXISTS credential_workspace_id_idx ON credential USING btree (workspace_id);


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS credential_id_idx;
DROP INDEX IF EXISTS credential_name_idx;
DROP INDEX IF EXISTS credential_workspace_id_idx;

DROP TABLE IF EXISTS credential;