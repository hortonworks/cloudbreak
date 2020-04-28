-- // CB-5653 single_resource_group
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_parameters
    ADD COLUMN IF NOT EXISTS resource_group_name varchar(255);

ALTER TABLE environment_parameters
    ADD COLUMN IF NOT EXISTS resource_group_creation varchar(20);
ALTER TABLE environment_parameters ALTER COLUMN resource_group_creation SET DEFAULT 'CREATE_NEW';
UPDATE environment_parameters SET resource_group_creation = 'CREATE_NEW' WHERE resource_group_creation IS NULL;

ALTER TABLE environment_parameters
    ADD COLUMN IF NOT EXISTS resource_group_single varchar(20);
ALTER TABLE environment_parameters ALTER COLUMN resource_group_single SET DEFAULT 'USE_MULTIPLE';
UPDATE environment_parameters SET resource_group_single = 'USE_MULTIPLE' WHERE resource_group_single IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_parameters
    DROP COLUMN IF EXISTS resource_group_name;

ALTER TABLE environment_parameters
    DROP COLUMN IF EXISTS resource_group_creation;

ALTER TABLE environment_parameters
    DROP COLUMN IF EXISTS resource_group_single;