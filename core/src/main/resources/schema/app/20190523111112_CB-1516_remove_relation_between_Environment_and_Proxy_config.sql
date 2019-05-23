-- // CB-1516 remove relation between Environment and Proxy config
-- Migration SQL that makes the change goes here.
DROP TABLE IF EXISTS env_proxy;


-- //@UNDO
-- SQL to undo the change goes here.
CREATE TABLE IF NOT EXISTS env_proxy (
    envid bigint NOT NULL,
    proxyid bigint NOT NULL
);
ALTER TABLE ONLY env_proxy ADD CONSTRAINT fk_env_proxy_envid FOREIGN KEY (envid) REFERENCES environment(id);
ALTER TABLE ONLY env_proxy ADD CONSTRAINT fk_env_proxy_proxyid FOREIGN KEY (proxyid) REFERENCES proxyconfig(id);
ALTER TABLE ONLY env_proxy ADD CONSTRAINT uk_env_proxy_envid_proxyid UNIQUE (envid, proxyid);
CREATE INDEX IF NOT EXISTS idx_env_proxy_envid ON env_proxy (envid);
CREATE INDEX IF NOT EXISTS idx_env_proxy_proxyid ON env_proxy (proxyid);

