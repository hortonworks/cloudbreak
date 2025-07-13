-- // CB-29916 Add index for instancemetadata status + ig id  because couple query depends on these as a where criteria
-- Migration SQL that makes the change goes here.

create index if not exists idx_instancemetadata_instancestatus_instancegroup_id on instancemetadata(instancestatus, instancegroup_id);

-- //@UNDO
-- SQL to undo the change goes here.

drop index if exists idx_instancemetadata_instancestatus_instancegroup_id;

