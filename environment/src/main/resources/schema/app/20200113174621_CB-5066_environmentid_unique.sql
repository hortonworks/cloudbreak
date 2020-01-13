-- // CB-5066 make environment_id index unique
-- Migration SQL that makes the change goes here.

DROP INDEX IF EXISTS environment_network_environment_id_idx;
CREATE UNIQUE INDEX environment_network_environment_id_idx ON environment_network USING btree (environment_id);

-- //@UNDO
-- SQL to undo the change goes here.
