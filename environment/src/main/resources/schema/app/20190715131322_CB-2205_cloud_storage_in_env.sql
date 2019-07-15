-- // CB-2205 Enhance Environment service to store Cloud Storage configuration

ALTER TABLE environment ADD COLUMN IF NOT EXISTS logcloudstorage text;

-- //@UNDO

ALTER TABLE environment DROP COLUMN IF EXISTS logcloudstorage;

