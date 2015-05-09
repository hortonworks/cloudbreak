-- // CLOUD-576_rename_longname_column_to_discoveryfqdn
-- Migration SQL that makes the change goes here.

alter table instancemetadata rename COLUMN longname to discoveryfqdn;



-- //@UNDO
-- SQL to undo the change goes here.

alter table instancemetadata rename COLUMN discoveryfqdn to longname;


