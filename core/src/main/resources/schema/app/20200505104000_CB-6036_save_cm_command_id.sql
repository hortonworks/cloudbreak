-- // CB-6036 Save CM command id
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS clustercommand(
    id bigserial NOT NULL,
    cluster_id bigint NOT NULL,
    command_id numeric NOT NULL,
    cluster_command_type VARCHAR(100) NOT NULL,
    created bigint NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY (cluster_id) REFERENCES cluster(id)
);

CREATE INDEX cluster_command_cluster_id_idx ON clustercommand (cluster_id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX cluster_command_cluster_id_idx;

DROP TABLE clustercommand;
