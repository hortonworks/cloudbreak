-- // schema changes for keystone v3
-- Migration SQL that makes the change goes here.
ALTER TABLE credential ADD COLUMN keystoneversion character varying(255);

ALTER TABLE credential ADD COLUMN keystoneauthscope character varying(255);

ALTER TABLE credential ADD COLUMN projectdomainname character varying(255);

ALTER TABLE credential ADD COLUMN domainname character varying(255);

ALTER TABLE credential ADD COLUMN projectname character varying(255);

ALTER TABLE credential ADD COLUMN userdomain character varying(255);

-- //@UNDO
-- SQL to undo the change goes here. 

ALTER TABLE credential DROP COLUMN keystoneversion;

ALTER TABLE credential DROP COLUMN keystoneauthscope;

ALTER TABLE credential DROP COLUMN projectdomainname;

ALTER TABLE credential DROP COLUMN domainname;

ALTER TABLE credential DROP COLUMN projectname;

ALTER TABLE credential DROP COLUMN userdomain;


