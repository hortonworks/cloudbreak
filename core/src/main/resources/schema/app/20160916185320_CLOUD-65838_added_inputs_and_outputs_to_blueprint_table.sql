-- // CLOUD-65838 added inputs and outputs to blueprint table
-- Migration SQL that makes the change goes here.


CREATE TABLE blueprint_parameter_inputs (
    name  varchar(255) NOT NULL,
    description        varchar(255),
    referenceConfiguration TEXT NOT NULL,
    blueprint_id bigint NOT NULL
);

ALTER TABLE blueprint_parameter_inputs
   ADD CONSTRAINT fk_blueprint_parameter_inputs_blueprint FOREIGN KEY (blueprint_id)
       REFERENCES blueprint (id);

CREATE TABLE blueprint_inputs (
    name  varchar(255) NOT NULL,
    propertyValue        TEXT NOT NULL,
    cluster_id bigint NOT NULL
);

ALTER TABLE blueprint_inputs
   ADD CONSTRAINT fk_blueprint_inputs_cluster FOREIGN KEY (cluster_id)
       REFERENCES cluster (id);

-- //@UNDO
-- SQL to undo the change goes here.

drop table blueprint_parameter_inputs;
drop table blueprint_inputs;

