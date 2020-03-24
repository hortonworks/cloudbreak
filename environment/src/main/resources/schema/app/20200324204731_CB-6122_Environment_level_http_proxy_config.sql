-- // CB-6122 Environment level http proxy config
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS proxyconfig_id BIGINT NULL;

ALTER TABLE ONLY environment ADD CONSTRAINT fk_environment_proxyconfig_id
  FOREIGN KEY (proxyconfig_id) REFERENCES proxyconfig(id);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP CONSTRAINT IF EXISTS fk_environment_proxyconfig_id;

ALTER TABLE environment DROP COLUMN IF EXISTS proxyconfig_id;