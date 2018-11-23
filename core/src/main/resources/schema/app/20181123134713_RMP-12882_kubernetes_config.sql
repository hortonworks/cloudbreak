-- // RMP-12882_kubernetes_config
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS kubernetesconfig (
	"id" BIGINT NOT NULL,
	"name" CHARACTER VARYING(255) NOT NULL,
	"configuration" CHARACTER VARYING(255) NOT NULL,
	"workspace_id" BIGINT NOT NULL,
	PRIMARY KEY ("id"),
	CONSTRAINT "kubernetesconfigname_in_org_unique" UNIQUE("name", "workspace_id")
);

CREATE SEQUENCE kubernetesconfig_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE INDEX kubernetesconfig_name_idx ON kubernetesconfig ("name");

CREATE INDEX kubernetesconfig_org_id_idx ON kubernetesconfig ("workspace_id");

CREATE INDEX idx_kubernetesconfig_workspace_id_name ON kubernetesconfig ("workspace_id", "name");

ALTER TABLE kubernetesconfig
	ADD CONSTRAINT fk_kubernetesconfig_organization FOREIGN KEY ("workspace_id")
	REFERENCES workspace ("id") MATCH SIMPLE
	ON DELETE NO ACTION
	ON UPDATE NO ACTION
;

CREATE TABLE IF NOT EXISTS env_kubernetes (
    envid bigint NOT NULL,
    kubernetesid bigint NOT NULL
);
ALTER TABLE ONLY env_kubernetes ADD CONSTRAINT fk_env_kubernetes_envid FOREIGN KEY (envid) REFERENCES environment(id);
ALTER TABLE ONLY env_kubernetes ADD CONSTRAINT fk_env_kubernetes_kubernetesid FOREIGN KEY (kubernetesid) REFERENCES kubernetesconfig(id);
ALTER TABLE ONLY env_kubernetes ADD CONSTRAINT uk_env_kubernetes_envid_kubernetesid UNIQUE (envid, kubernetesid);
CREATE INDEX IF NOT EXISTS idx_env_kubernetes_envid ON env_kubernetes (envid);
CREATE INDEX IF NOT EXISTS idx_env_kubernetes_kubernetesid ON env_kubernetes (kubernetesid);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_env_kubernetes_kubernetesid;
DROP INDEX IF EXISTS idx_env_kubernetes_envid;
ALTER TABLE ONLY env_kubernetes DROP CONSTRAINT IF EXISTS uk_env_kubernetes_envid_kubernetesid;
ALTER TABLE ONLY env_kubernetes DROP CONSTRAINT IF EXISTS fk_env_kubernetes_kubernetesid;
ALTER TABLE ONLY env_kubernetes DROP CONSTRAINT IF EXISTS fk_env_kubernetes_envid;
DROP TABLE IF EXISTS env_kubernetes;

DROP TABLE IF EXISTS kubernetesconfig;

DROP SEQUENCE IF EXISTS kubernetesconfig_id_seq;
