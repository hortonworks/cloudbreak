-- // RMP-12337 - add tenant to user
-- Migration SQL that makes the change goes here.

alter table periscope_user add column tenant VARCHAR (255);
alter table periscope_user alter column account drop not null;

-- //@UNDO
-- SQL to undo the change goes here.

alter table periscope_user drop column tenant;
alter table periscope_user alter column account set not null;

