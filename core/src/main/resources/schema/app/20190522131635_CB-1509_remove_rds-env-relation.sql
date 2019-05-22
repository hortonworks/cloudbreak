-- // Remove RDS - Environment relation from Cloudbreak
-- Migration SQL that makes the change goes here.

DROP INDEX IF EXISTS idx_env_rds_rdsid;
DROP INDEX IF EXISTS idx_env_rds_envid;

DROP TABLE IF EXISTS env_rds;

 -- //@UNDO
-- SQL to undo the change goes here.

CREATE TABLE IF NOT EXISTS env_rds (
    envid bigint NOT NULL,
    rdsid bigint NOT NULL
);
ALTER TABLE ONLY env_rds ADD CONSTRAINT fk_env_rds_envid FOREIGN KEY (envid) REFERENCES environment(id);
ALTER TABLE ONLY env_rds ADD CONSTRAINT fk_env_rds_rdsid FOREIGN KEY (rdsid) REFERENCES rdsconfig(id);
ALTER TABLE ONLY env_rds ADD CONSTRAINT uk_env_rds_envid_rdsid UNIQUE (envid, rdsid);
CREATE INDEX IF NOT EXISTS idx_env_rds_envid ON env_rds (envid);
CREATE INDEX IF NOT EXISTS idx_env_rds_rdsid ON env_rds (rdsid);
