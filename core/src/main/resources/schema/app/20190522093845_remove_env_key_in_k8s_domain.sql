-- // remove env key in k8s domain
-- Migration SQL that makes the change goes here.

DROP TABLE IF EXISTS env_kubernetes;

-- //@UNDO
-- SQL to undo the change goes here.

CREATE TABLE IF NOT EXISTS env_kubernetes (
    envid bigint NOT NULL,
    kubernetesid bigint NOT NULL
);
ALTER TABLE ONLY env_kubernetes ADD CONSTRAINT fk_env_kubernetes_envid FOREIGN KEY (envid) REFERENCES environment(id);
ALTER TABLE ONLY env_kubernetes ADD CONSTRAINT fk_env_kubernetes_kubernetesid FOREIGN KEY (kubernetesid) REFERENCES kubernetesconfig(id);
ALTER TABLE ONLY env_kubernetes ADD CONSTRAINT uk_env_kubernetes_envid_kubernetesid UNIQUE (envid, kubernetesid);
CREATE INDEX IF NOT EXISTS idx_env_kubernetes_envid ON env_kubernetes (envid);
CREATE INDEX IF NOT EXISTS idx_env_kubernetes_kubernetesid ON env_kubernetes (kubernetesid);
