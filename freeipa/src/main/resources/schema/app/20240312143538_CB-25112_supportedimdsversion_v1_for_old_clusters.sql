-- // CB-25112 set supportedimdsversion to v1 for old AWS clusters
-- Migration SQL that makes the change goes here.

UPDATE stack SET supportedimdsversion = 'v1' WHERE supportedimdsversion IS NULL AND cloudplatform LIKE '%AWS%';

-- //@UNDO
-- SQL to undo the change goes here.
