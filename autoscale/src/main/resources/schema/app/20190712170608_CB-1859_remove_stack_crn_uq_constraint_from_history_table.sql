-- // CB-1859 remove stack crn uq constraint from history table
-- Migration SQL that makes the change goes here.
ALTER TABLE "history" DROP CONSTRAINT "history_cb_stack_crn_uq";

-- //@UNDO
-- SQL to undo the change goes here.


