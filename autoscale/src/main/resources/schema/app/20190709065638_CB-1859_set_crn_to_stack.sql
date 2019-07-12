-- // CB-1859 set crn to stack
-- Migration SQL that makes the change goes here.
ALTER TABLE "cluster" ADD COLUMN IF NOT EXISTS cb_stack_crn VARCHAR(255);

UPDATE "cluster"
SET cb_stack_crn = CONCAT('crn:altus:cloudbreak:us-west-1:stack:', "cluster".cb_stack_id)
FROM (SELECT *
	  FROM "cluster" c
	  WHERE cb_stack_crn IS NULL) AS SQ;

ALTER TABLE "cluster" DROP CONSTRAINT "uc_periscope_cluster_cb_stack";

ALTER TABLE "cluster" ALTER COLUMN cb_stack_crn SET NOT NULL;

ALTER TABLE "cluster" ADD CONSTRAINT cluster_cb_stack_crn_uq UNIQUE (cb_stack_crn);

ALTER TABLE "history" ADD COLUMN IF NOT EXISTS cb_stack_crn VARCHAR(255);

UPDATE "history"
SET cb_stack_crn = CONCAT('crn:altus:cloudbreak:us-west-1:stack:', "history".cb_stack_id)
FROM (SELECT *
	  FROM "history" h
	  WHERE cb_stack_crn IS NULL) AS SQ;

ALTER TABLE "history" ALTER COLUMN cb_stack_crn SET NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE "cluster" DROP COLUMN IF EXISTS cb_stack_crn;

ALTER TABLE "history" DROP COLUMN IF EXISTS cb_stack_crn;

