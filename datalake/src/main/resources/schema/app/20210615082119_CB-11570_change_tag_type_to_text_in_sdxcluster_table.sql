-- // CB-11570 Change tag type to text in sdxcluster table
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster ALTER tags TYPE TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

-- Left blank, can't rollback if longer data was inserted therefore we leave tags column as text
