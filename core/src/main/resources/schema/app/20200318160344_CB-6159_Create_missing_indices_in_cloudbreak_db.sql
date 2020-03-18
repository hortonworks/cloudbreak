-- // CB-6159 Create missing indices in cloudbreak db
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS idx_structuredevent_resourceid ON structuredevent(resourceid) where resourceid is not null;
CREATE INDEX IF NOT EXISTS idx_structuredevent_resourcecrn ON structuredevent(resourcecrn) where resourcecrn is not null;
CREATE INDEX IF NOT EXISTS idx_securityrule_securitygroupid ON securityrule(securitygroup_id);
CREATE INDEX IF NOT EXISTS idx_securitygroup_securitygroupids_securitygroupid ON securitygroup_securitygroupids(securitygroup_id);
CREATE INDEX IF NOT EXISTS idx_gateway_clusterid ON gateway(cluster_id);
CREATE INDEX IF NOT EXISTS idx_gateway_topology_gatewayid ON gateway_topology(gateway_id);
CREATE INDEX IF NOT EXISTS idx_topology_records_topologyid ON topology_records(topology_id);
CREATE INDEX IF NOT EXISTS idx_securityconfig_saltsecurityconfigid ON securityconfig(saltsecurityconfig_id);
CREATE INDEX IF NOT EXISTS idx_volumetemplate_templateid ON volumetemplate(template_id);
CREATE INDEX IF NOT EXISTS idx_clustercomponent_clusterid ON clustercomponent(cluster_id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_structuredevent_resourceid;
DROP INDEX IF EXISTS idx_structuredevent_resourcecrn;
DROP INDEX IF EXISTS idx_securityrule_securitygroupid;
DROP INDEX IF EXISTS idx_securitygroup_securitygroupids_securitygroupid;
DROP INDEX IF EXISTS idx_gateway_clusterid;
DROP INDEX IF EXISTS idx_gateway_topology_gatewayid;
DROP INDEX IF EXISTS idx_topology_records_topologyid;
DROP INDEX IF EXISTS idx_securityconfig_saltsecurityconfigid;
DROP INDEX IF EXISTS idx_volumetemplate_templateid;
DROP INDEX IF EXISTS idx_clustercomponent_clusterid;

