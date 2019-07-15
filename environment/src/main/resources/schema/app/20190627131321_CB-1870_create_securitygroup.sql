-- // CB-1870 adding security groups and CIDR to environment

ALTER TABLE environment
    ADD COLUMN IF NOT EXISTS securitygroup_id_knox VARCHAR(255),
    ADD COLUMN IF NOT EXISTS securitygroup_id_default VARCHAR(255),
    ADD COLUMN IF NOT EXISTS cidr VARCHAR(43);

UPDATE environment set cidr='0.0.0.0/0';

-- //@UNDO

ALTER TABLE environment
    DROP COLUMN IF EXISTS securitygroup_id_knox,
    DROP COLUMN IF EXISTS securitygroup_id_default,
    DROP COLUMN IF EXISTS cidr;
