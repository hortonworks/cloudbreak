-- // CLOUD-61094_extend_cloudbreak_usage_with_blueprintid_and_blueprintname
-- Migration SQL that makes the change goes here.

ALTER TABLE cloudbreakusage ADD COLUMN blueprintid BIGINT;
ALTER TABLE cloudbreakusage ADD COLUMN blueprintname VARCHAR(255);

UPDATE cloudbreakusage
  SET (blueprintid, blueprintname) = (cloudbreakevent.blueprintid, cloudbreakevent.blueprintname)
  FROM cloudbreakevent WHERE cloudbreakevent.stackid = cloudbreakusage.stackid;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cloudbreakusage DROP COLUMN blueprintid;
ALTER TABLE cloudbreakusage DROP COLUMN blueprintname;
