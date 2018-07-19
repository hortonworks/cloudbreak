-- // BUG-106826_authorization_domain_model
-- Migration SQL that makes the change goes here.

-- creating new authorization domain:

CREATE TABLE IF NOT EXISTS tenant (
    id                      bigserial NOT NULL,
    name                    VARCHAR (255) NOT NULL,

    CONSTRAINT              tenant_id                               PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS organization (
    id                      bigserial NOT NULL,
    name                    VARCHAR (255),
    tenant_id               int8 NOT NULL,

    CONSTRAINT              pk_organization_id                      PRIMARY KEY (id),
    CONSTRAINT              fk_organization_tenant                  FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

CREATE TABLE IF NOT EXISTS users (
    id                      bigserial NOT NULL,
    name                    VARCHAR (255),
    email                   VARCHAR (255) NOT NULL UNIQUE,
    company                 VARCHAR (255),
    tenant_permissions      TEXT NOT NULL,
    cloudbreak_permissions  TEXT,
    tenant_id               int8 NOT NULL,

    CONSTRAINT              pk_user_id                              PRIMARY KEY (id),
    CONSTRAINT              fk_user_tenant                          FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

CREATE TABLE IF NOT EXISTS user_org_permissions (
    id                      bigserial NOT NULL,
    permissions             TEXT NOT NULL,
    user_id                 int8 NOT NULL,
    organization_id         int8 NOT NULL,

    CONSTRAINT              pk_user_to_organization_id              PRIMARY KEY (id),
    CONSTRAINT              fk_user_to_organization_user            FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT              fk_user_to_organization_org             FOREIGN KEY (organization_id) REFERENCES organization(id)
);

-- adding organization foreign key to resources:

ALTER TABLE stack
    ADD organization_id int8,
    ADD CONSTRAINT stackname_in_org_unique UNIQUE (name, organization_id),
    ADD CONSTRAINT fk_stack_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

ALTER TABLE recipe
    ADD organization_id int8,
    ADD CONSTRAINT recipename_in_org_unique UNIQUE (name, organization_id),
    ADD CONSTRAINT fk_recipe_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

ALTER TABLE blueprint
    ADD organization_id int8,
    ADD CONSTRAINT blueprintname_in_org_unique UNIQUE (name, organization_id),
    ADD CONSTRAINT fk_blueprint_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

ALTER TABLE credential
    ADD organization_id int8,
    ADD CONSTRAINT credentialname_in_org_unique UNIQUE (name, organization_id),
    ADD CONSTRAINT fk_credential_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

ALTER TABLE managementpack
    ADD organization_id int8,
    ADD CONSTRAINT managementpackname_in_org_unique UNIQUE (name, organization_id),
    ADD CONSTRAINT fk_managementpack_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

ALTER TABLE ldapconfig
    ADD organization_id int8,
    ADD CONSTRAINT ldapconfigname_in_org_unique UNIQUE (name, organization_id),
    ADD CONSTRAINT fk_ldapconfig_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

ALTER TABLE rdsconfig
    ADD organization_id int8,
    ADD CONSTRAINT rdsconfigname_in_org_unique UNIQUE (name, organization_id),
    ADD CONSTRAINT fk_rdsconfig_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

ALTER TABLE imagecatalog
    ADD organization_id int8,
    ADD CONSTRAINT imagecatalogname_in_org_unique UNIQUE (name, organization_id),
    ADD CONSTRAINT fk_imagecatalog_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

ALTER TABLE proxyconfig
    ADD organization_id int8,
    ADD CONSTRAINT proxyconfigname_in_org_unique UNIQUE (name, organization_id),
    ADD CONSTRAINT fk_proxyconfig_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack
    DROP CONSTRAINT IF EXISTS fk_stack_organization,
    DROP CONSTRAINT IF EXISTS stackname_in_org_unique,
    DROP COLUMN IF EXISTS organization_id;

ALTER TABLE recipe
    DROP CONSTRAINT IF EXISTS fk_recipe_organization,
    DROP CONSTRAINT IF EXISTS recipename_in_org_unique,
    DROP COLUMN IF EXISTS organization_id;

ALTER TABLE blueprint
    DROP CONSTRAINT IF EXISTS fk_blueprint_organization,
    DROP CONSTRAINT IF EXISTS blueprintname_in_org_unique,
    DROP COLUMN IF EXISTS organization_id;

ALTER TABLE credential
    DROP CONSTRAINT IF EXISTS fk_credential_organization,
    DROP CONSTRAINT IF EXISTS credentialname_in_org_unique,
    DROP COLUMN IF EXISTS organization_id;

ALTER TABLE managementpack
    DROP CONSTRAINT IF EXISTS fk_managementpack_organization,
    DROP CONSTRAINT IF EXISTS managementpackname_in_org_unique,
    DROP COLUMN IF EXISTS organization_id;

ALTER TABLE ldapconfig
    DROP CONSTRAINT IF EXISTS fk_ldapconfig_organization,
    DROP CONSTRAINT IF EXISTS ldapconfigname_in_org_unique,
    DROP COLUMN IF EXISTS organization_id;

ALTER TABLE rdsconfig
    DROP CONSTRAINT IF EXISTS fk_rdsconfig_organization,
    DROP CONSTRAINT IF EXISTS rdsconfigname_in_org_unique,
    DROP COLUMN IF EXISTS organization_id;

ALTER TABLE imagecatalog
    DROP CONSTRAINT IF EXISTS fk_imagecatalog_organization,
    DROP CONSTRAINT IF EXISTS imagecatalogname_in_org_unique,
    DROP COLUMN IF EXISTS organization_id;

ALTER TABLE proxyconfig
    DROP CONSTRAINT IF EXISTS fk_proxyconfig_organization,
    DROP CONSTRAINT IF EXISTS proxyconfigname_in_org_unique,
    DROP COLUMN IF EXISTS organization_id;

DROP TABLE IF EXISTS user_org_permissions;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS organization;
DROP TABLE IF EXISTS tenant;
