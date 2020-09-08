-- // single_resource_group_with_storage
-- Migration SQL that makes the change goes here.
ALTER TABLE environment_parameters ALTER COLUMN resource_group_single TYPE varchar(50) USING resource_group_single::varchar;

-- //@UNDO
-- SQL to undo the change goes here.
UPDATE environment_parameters
	SET resource_group_single='USE_SINGLE'
	WHERE resource_group_single='USE_SINGLE_WITH_DEDICATED_STORAGE_ACCOUNT';
ALTER TABLE environment_parameters ALTER COLUMN resource_group_single TYPE varchar(20) USING resource_group_single::varchar;