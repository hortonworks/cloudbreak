-- // CB-24874 add cluster status table
-- Migration SQL that makes the change goes here.

CREATE TABLE externalized_compute_cluster_status (id bigserial NOT NULL,
                     externalizedcomputecluster_id bigint,
                     created bigint,
                     statusReason TEXT,
                     status character varying(255) NOT NULL,
                     PRIMARY KEY (id));

CREATE INDEX index_status_externalizedcluster_status ON externalized_compute_cluster_status (externalizedcomputecluster_id, status);
CREATE INDEX index_status_externalizedcluster ON externalized_compute_cluster_status (externalizedcomputecluster_id);
CREATE SEQUENCE IF NOT EXISTS status_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS index_status_externalizedcluster_status;
DROP INDEX IF EXISTS index_status_externalizedcluster;
DROP TABLE IF EXISTS status;
DROP SEQUENCE IF EXISTS status_id_seq;
