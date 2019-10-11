-- // CB-3956 preview template support
-- Migration SQL that makes the change goes here.

ALTER TABLE clustertemplate ADD COLUMN featureState varchar(30);
UPDATE clustertemplate SET featureState='RELEASED';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE clustertemplate DROP COLUMN featureState;

