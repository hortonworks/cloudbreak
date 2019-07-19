-- // CB-2455 add IDBroker mapping type

ALTER TABLE environment
    ADD COLUMN idbroker_mapping_source VARCHAR(10);

UPDATE environment set idbroker_mapping_source='NONE';

-- //@UNDO

ALTER TABLE environment
    DROP COLUMN IF EXISTS idbroker_mapping_source;
