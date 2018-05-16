-- // BUG-102890_Make_periscope_cluster_cb_stack_unique
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD CONSTRAINT uc_periscope_cluster_cb_stack UNIQUE (cb_stack_id);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP CONSTRAINT IF EXISTS uc_periscope_cluster_cb_stack;