-- // CLOUD-49843 gather hypervisor data
-- Migration SQL that makes the change goes here.

ALTER TABLE instancemetadata DROP COLUMN volumecount;
ALTER TABLE instancemetadata ADD COLUMN hypervisor VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancemetadata ADD COLUMN volumecount INTEGER;
ALTER TABLE instancemetadata DROP COLUMN hypervisor;