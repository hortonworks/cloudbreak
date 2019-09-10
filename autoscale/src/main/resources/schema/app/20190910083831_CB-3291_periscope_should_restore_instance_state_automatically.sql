-- // CB-3291 periscope should restore instance state automatically
-- Migration SQL that makes the change goes here.

CREATE TABLE failed_node (
    id bigint NOT NULL,
    created BIGINT NOT NULL DEFAULT (date_part('epoch'::text, now()) * (1000)::double precision),
    cluster_id bigint NOT NULL,
    name character varying(255),
    PRIMARY KEY (id)
);

ALTER TABLE ONLY failed_node ADD CONSTRAINT fk_failed_node_cluster_id FOREIGN KEY (cluster_id) REFERENCES cluster(id);

CREATE INDEX idx_failed_node_cluster_id ON failed_node (cluster_id);

CREATE SEQUENCE failed_node_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE failed_node ALTER COLUMN id SET DEFAULT nextval ('failed_node_id_seq');

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ONLY failed_node DROP CONSTRAINT fk_failed_node_cluster_id:

DROP INDEX idx_failed_node_cluster_id;

DROP SEQUENCE failed_node_id_seq;

DROP TABLE failed_node;
