-- // CB-8663 add new IdBroker table.
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE idbroker_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE TABLE idbroker (
    id              bigint NOT NULL DEFAULT nextval('idbroker_id_seq'),
    cluster_id      bigint NOT NULL,
    workspace_id    int8 NOT NUll,
    signkey         TEXT,
    signcert        TEXT,
    signpub         TEXT,
    mastersecret    VARCHAR(255),
    PRIMARY KEY (id)
);

ALTER TABLE idbroker
   ADD CONSTRAINT fk_idbroker_cluster FOREIGN KEY (cluster_id)
        REFERENCES cluster (id);
ALTER TABLE idbroker
   ADD CONSTRAINT fk_idbroker_workspace FOREIGN KEY (workspace_id)
        REFERENCES workspace(id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE idbroker;
DROP SEQUENCE idbroker_id_seq;

