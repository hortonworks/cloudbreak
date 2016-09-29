-- // CLOUD-66496 convert blueprints parameters collection to a json field
-- Migration SQL that makes the change goes here.

ALTER TABLE ONLY blueprint_parameter_inputs DROP CONSTRAINT IF EXISTS fk_blueprint_parameter_inputs_blueprint;
DROP TABLE IF EXISTS blueprint_parameter_inputs;

ALTER TABLE blueprint ADD COLUMN inputparameters TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE blueprint DROP COLUMN IF EXISTS inputparameters;

CREATE TABLE blueprint_parameter_inputs (
    name  varchar(255) NOT NULL,
    description        varchar(255),
    referenceConfiguration TEXT NOT NULL,
    blueprint_id bigint NOT NULL
);

ALTER TABLE blueprint_parameter_inputs
   ADD CONSTRAINT fk_blueprint_parameter_inputs_blueprint FOREIGN KEY (blueprint_id)
       REFERENCES blueprint (id);

