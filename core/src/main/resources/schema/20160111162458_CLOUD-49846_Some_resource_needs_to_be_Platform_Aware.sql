-- // CLOUD-49846 Some resource needs to be Platform Aware
-- Migration SQL that makes the change goes here.

ALTER TABLE credential ADD topology_id bigint;
ALTER TABLE credential ADD CONSTRAINT fk_credential_topology FOREIGN KEY (topology_id) REFERENCES topology (id);

ALTER TABLE network ADD topology_id bigint;
ALTER TABLE network ADD CONSTRAINT fk_network_topology FOREIGN KEY (topology_id) REFERENCES topology (id);

ALTER TABLE template ADD topology_id bigint;
ALTER TABLE template ADD CONSTRAINT fk_template_topology FOREIGN KEY (topology_id) REFERENCES topology (id);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE credential DROP COLUMN topology_id;
ALTER TABLE network DROP COLUMN topology_id;
ALTER TABLE template DROP COLUMN topology_id;

