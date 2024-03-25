-- // CB-25216 set supportedimdsversion to v1 for old AWS clusters again
-- Migration SQL that makes the change goes here.

UPDATE stack SET supportedimdsversion = 'v1' WHERE supportedimdsversion IS NULL AND cloudplatform LIKE '%AWS%';

-- //@UNDO
-- SQL to undo the change goes here.
