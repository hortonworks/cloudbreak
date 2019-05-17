-- // Add Tenant Column

-- This change is to add tenant col into stack table.


ALTER TABLE stack
    ADD tenant VARCHAR(255);

CREATE INDEX tenant_idx
    ON stack (tenant);

CREATE INDEX environment_tenant_idx
    ON stack (environment, tenant);

CREATE UNIQUE INDEX name_environment_tenant_uindex
    ON stack (name, environment, tenant);

DROP INDEX stack_name_environment_uindex;


-- //@UNDO
CREATE UNIQUE INDEX stack_name_environment_uindex
    ON stack (name, environment);


DROP INDEX tenant_idx;

DROP INDEX environment_tenant_idx;

DROP INDEX name_environment_tenant_uindex;

ALTER TABLE stack DROP COLUMN tenant;