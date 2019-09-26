-- // CB-3003 S3Guard DynamoDB table cleanup
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_parameters
    ADD COLUMN IF NOT EXISTS s3guard_dynamo_table_creation varchar(20);

UPDATE environment_parameters
    SET s3guard_dynamo_table_creation = 'USE_EXISTING';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_parameters
    DROP COLUMN IF EXISTS s3guard_dynamo_table_creation;
