-- // CLOUD-94185 append UUID to archived image catalogs
-- Migration SQL that makes the change goes here.
update imagecatalog set name = name || '_' || uuid_in(md5(random()::text || clock_timestamp()::text)::cstring) where archived = true;


-- //@UNDO
-- SQL to undo the change goes here.


