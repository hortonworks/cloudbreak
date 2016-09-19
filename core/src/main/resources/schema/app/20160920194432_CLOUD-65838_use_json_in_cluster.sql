-- // CLOUD-65838 use json in cluster
-- Migration SQL that makes the change goes here.

ALTER TABLE blueprint_inputs DROP CONSTRAINT fk_blueprint_inputs_cluster;
DROP TABLE blueprint_inputs;

ALTER TABLE cluster ADD COLUMN blueprintinputs TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN blueprintinputs;

CREATE TABLE blueprint_inputs (
    name  varchar(255) NOT NULL,
    propertyValue        TEXT NOT NULL,
    cluster_id bigint NOT NULL
);

ALTER TABLE blueprint_inputs
   ADD CONSTRAINT fk_blueprint_inputs_cluster FOREIGN KEY (cluster_id)
       REFERENCES cluster (id);