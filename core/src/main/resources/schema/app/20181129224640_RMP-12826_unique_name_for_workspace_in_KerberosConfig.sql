-- // RMP-12826 unique name for workspace in KerberosConfig
-- Migration SQL that makes the change goes here.

DROP INDEX idx_kerberosconfig_workspace_id_name;
CREATE UNIQUE INDEX idx_kerberosconfig_workspace_id_name ON kerberosconfig(name text_ops,workspace_id int8_ops);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX idx_kerberosconfig_workspace_id_name;
CREATE INDEX idx_kerberosconfig_workspace_id_name ON kerberosconfig(workspace_id int8_ops,name text_ops);

