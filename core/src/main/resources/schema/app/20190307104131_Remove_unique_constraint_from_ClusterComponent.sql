-- // Remove unique constraint from ClusterComponent
-- Migration SQL that makes the change goes here.

ALTER TABLE clustercomponent DROP CONSTRAINT IF EXISTS uk_clustercomponent_componenttype_name_cluster;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE clustercomponent ADD CONSTRAINT uk_clustercomponent_componenttype_name_cluster UNIQUE (componenttype, name, cluster_id);


