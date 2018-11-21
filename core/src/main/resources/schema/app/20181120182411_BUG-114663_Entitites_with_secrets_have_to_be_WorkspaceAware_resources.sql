-- // BUG-114663 Entitites with secrets have to be WorkspaceAware resources
-- Migration SQL that makes the change goes here.

-- gateway resource
ALTER TABLE gateway
    ADD COLUMN IF NOT EXISTS workspace_id int8,
    ADD CONSTRAINT fk_gateway_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id);

CREATE INDEX IF NOT EXISTS gateway_workspace_id_idx ON gateway (workspace_id);

UPDATE gateway SET workspace_id=cluster.workspace_id FROM cluster WHERE cluster.id=gateway.cluster_id;

ALTER TABLE gateway ALTER COLUMN workspace_id SET NOT NULL;

-- kerberosconfig resource
ALTER TABLE kerberosconfig
    ADD COLUMN IF NOT EXISTS workspace_id int8,
    ADD CONSTRAINT fk_kerberosconfig_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id);

CREATE INDEX IF NOT EXISTS kerberosconfig_workspace_id_idx ON kerberosconfig (workspace_id);

UPDATE kerberosconfig SET workspace_id=cluster.workspace_id FROM cluster WHERE cluster.kerberosconfig_id=kerberosconfig.id;

ALTER TABLE kerberosconfig ALTER COLUMN workspace_id SET NOT NULL;

-- generatedrecipe resource
ALTER TABLE generatedrecipe
    ADD COLUMN IF NOT EXISTS workspace_id int8,
    ADD CONSTRAINT fk_generatedrecipe_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id);

CREATE INDEX IF NOT EXISTS generatedrecipe_workspace_id_idx ON generatedrecipe (workspace_id);

UPDATE generatedrecipe SET workspace_id=cluster.workspace_id
    FROM hostgroup INNER JOIN cluster on hostgroup.cluster_id=cluster.id WHERE generatedrecipe.hostgroup_id=hostgroup.id;

ALTER TABLE generatedrecipe ALTER COLUMN workspace_id SET NOT NULL;

-- securityconfig resource
ALTER TABLE securityconfig
    ADD COLUMN IF NOT EXISTS workspace_id int8,
    ADD CONSTRAINT fk_securityconfig_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id);

CREATE INDEX IF NOT EXISTS securityconfig_workspace_id_idx ON securityconfig (workspace_id);

UPDATE securityconfig SET workspace_id=stack.workspace_id FROM stack WHERE stack.id=securityconfig.stack_id;

ALTER TABLE securityconfig ALTER COLUMN workspace_id SET NOT NULL;

-- saltsecurityconfig resource
ALTER TABLE saltsecurityconfig
    ADD COLUMN IF NOT EXISTS workspace_id int8,
    ADD CONSTRAINT fk_saltsecurityconfig_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id);

CREATE INDEX IF NOT EXISTS saltsecurityconfig_workspace_id_idx ON saltsecurityconfig (workspace_id);

UPDATE saltsecurityconfig SET workspace_id=securityconfig.workspace_id FROM securityconfig WHERE securityconfig.saltsecurityconfig_id=saltsecurityconfig.id;

ALTER TABLE saltsecurityconfig ALTER COLUMN workspace_id SET NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

-- securityconfig resource
DROP INDEX IF EXISTS       securityconfig_workspace_id_idx;

ALTER TABLE securityconfig
    DROP CONSTRAINT IF EXISTS fk_securityconfig_workspace,
    DROP COLUMN IF EXISTS workspace_id;

-- saltsecurityconfig resource
DROP INDEX IF EXISTS       saltsecurityconfig_workspace_id_idx;

ALTER TABLE saltsecurityconfig
    DROP CONSTRAINT IF EXISTS fk_saltsecurityconfig_workspace,
    DROP COLUMN IF EXISTS workspace_id;

-- generatedrecipe resource
DROP INDEX IF EXISTS       generatedrecipe_workspace_id_idx;

ALTER TABLE generatedrecipe
    DROP CONSTRAINT IF EXISTS fk_generatedrecipe_workspace,
    DROP COLUMN IF EXISTS workspace_id;

-- kerberosconfig resource
DROP INDEX IF EXISTS       kerberosconfig_workspace_id_idx;

ALTER TABLE kerberosconfig
    DROP CONSTRAINT IF EXISTS fk_kerberosconfig_workspace,
    DROP COLUMN IF EXISTS workspace_id;

-- gateway resource
DROP INDEX IF EXISTS       gateway_workspace_id_idx;

ALTER TABLE gateway
    DROP CONSTRAINT IF EXISTS fk_gateway_workspace,
    DROP COLUMN IF EXISTS workspace_id;
