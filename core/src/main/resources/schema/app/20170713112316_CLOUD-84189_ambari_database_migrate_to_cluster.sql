-- // CLOUD-84189 ambari database migrate to cluster
-- Migration SQL that makes the change goes here.

INSERT INTO clustercomponent (componenttype, name, cluster_id, attributes)
        SELECT c.componenttype, c.name, cl.id, c.attributes
        FROM component c
        INNER JOIN cluster cl ON cl.stack_id=c.stack_id
        WHERE ((c.componentType='AMBARI_REPO_DETAILS' OR c.componentType='HDP_REPO_DETAILS' OR c.componentType='AMBARI_DATABASE_DETAILS')
        AND NOT EXISTS ((SELECT 1 FROM clustercomponent WHERE clustercomponent.componentType = c.componentType AND clustercomponent.cluster_id = cl.id)));

-- //@UNDO
-- SQL to undo the change goes here.
-- Rollback of this transaction is not relevant because we does not delete the component table data