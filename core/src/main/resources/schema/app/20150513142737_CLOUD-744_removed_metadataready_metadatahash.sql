-- // CLOUD-744_removed_metadataready_metadatahash
-- Migration SQL that makes the change goes here.

alter table stack drop column metadataready;
alter table stack drop column hash;
alter table cloudbreakevent alter column eventmessage TYPE text;

-- //@UNDO
-- SQL to undo the change goes here.


