-- // CLOUD-73592 ability to specify more rdsconfig
-- Migration SQL that makes the change goes here.

CREATE TABLE cluster_rdsconfig (
    clusters_id bigint NOT NULL,
    rdsconfigs_id bigint NOT NULL
);

ALTER TABLE ONLY cluster_rdsconfig ADD CONSTRAINT cluster_rdsconfig_pkey PRIMARY KEY (clusters_id, rdsconfigs_id);
ALTER TABLE ONLY cluster_rdsconfig ADD CONSTRAINT fk_cluster_rdsconfig_cluster_id FOREIGN KEY (clusters_id) REFERENCES cluster(id);
ALTER TABLE ONLY cluster_rdsconfig ADD CONSTRAINT fk_cluster_rdsconfig_rdsconfig_id FOREIGN KEY (rdsconfigs_id) REFERENCES rdsconfig(id);

INSERT INTO cluster_rdsconfig(clusters_id, rdsconfigs_id) SELECT id, rdsconfig_id FROM cluster WHERE rdsconfig_id IS NOT NULL;

ALTER TABLE cluster DROP COLUMN rdsconfig_id;

-- //@UNDO
-- SQL to undo the change goes here.


ALTER TABLE cluster ADD COLUMN rdsconfig_id BIGINT REFERENCES rdsconfig(id);

DROP TABLE cluster_rdsconfig;