-- // remove credential from stack and add as CRN
-- Migration SQL that makes the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS credential_id;
ALTER TABLE stack ADD COLUMN IF NOT EXISTS credentialcrn varchar(255);

ALTER TABLE environment DROP CONSTRAINT IF EXISTS fk_environment_credential_id;

ALTER TABLE userprofile DROP COLUMN IF EXISTS credential_id;

DROP TABLE IF EXISTS userprofile_credential;
DROP TABLE IF EXISTS credential;

-- //@UNDO
-- SQL to undo the change goes here.

CREATE TABLE "public"."credential" (
    "id" bigserial NOT NULL,
    "account" varchar(255),
    "description" text,
    "name" varchar(255) NOT NULL,
    "owner" varchar(255),
    "publicinaccount" bool NOT NULL DEFAULT false,
    "publickey" text,
    "archived" bool DEFAULT false,
    "loginusername" text,
    "attributes" text,
    "cloudplatform" varchar(255) NOT NULL,
    "topology_id" int8,
    "workspace_id" int8,
    "govcloud" bool DEFAULT false,
    CONSTRAINT "fk_credential_organization" FOREIGN KEY ("workspace_id") REFERENCES "public"."workspace"("id"),
    CONSTRAINT "fk_credential_topology" FOREIGN KEY ("topology_id") REFERENCES "public"."topology"("id"),
    PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX credential_id_idx ON credential USING btree (id);
CREATE INDEX credential_name_idx ON credential USING btree (name);
CREATE INDEX credential_org_id_idx ON credential USING btree (workspace_id);

ALTER TABLE stack ADD COLUMN IF NOT EXISTS credential_id bigint;
ALTER TABLE ONLY stack ADD CONSTRAINT fk_stack_credential_id FOREIGN KEY (credential_id) REFERENCES credential(id);

ALTER TABLE stack DROP COLUMN IF EXISTS credentialcrn;

CREATE TABLE userprofile_credential (
    userprofile_id bigint NOT NULL,
    defaultcredentials_id bigint NOT NULL
);

