-- // CB-20943 Remove the secrets from the userdata script which are stored in the database
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS userdata (
    id                      bigserial NOT NULL,
    stack_id                bigint,
    accountid               VARCHAR (255),
    coreuserdata            TEXT,
    gatewayuserdata         TEXT,
    PRIMARY KEY (id)
);

ALTER TABLE ONLY userdata ADD CONSTRAINT fk_userdata_stack_id FOREIGN KEY (stack_id) REFERENCES stack(id);
CREATE SEQUENCE IF NOT EXISTS userdata_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE INDEX IF NOT EXISTS idx_userdata_stackid ON userdata(stack_id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE userdata;

