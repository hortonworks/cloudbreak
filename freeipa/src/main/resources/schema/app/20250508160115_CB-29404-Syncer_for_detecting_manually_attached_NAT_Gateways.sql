-- // CB-29404-Syncer for detecting manually attached NAT Gateways
-- Migration SQL that makes the change goes here.
UPDATE resource r
SET attributes = (
    SELECT n.attributes::jsonb || '{"attributeType":"com.sequenceiq.cloudbreak.cloud.model.NetworkAttributes"}'::jsonb
    FROM network n
             JOIN stack s ON s.network_id = n.id
    WHERE s.id = r.resource_stack
)
WHERE r.resourcetype = 'AZURE_NETWORK';


-- //@UNDO
-- SQL to undo the change goes here.
UPDATE resource r
SET attributes = NULL
WHERE r.resourcetype = 'AZURE_NETWORK'
AND attributes::jsonb ->> 'attributeType' = 'com.sequenceiq.cloudbreak.cloud.model.NetworkAttributes';


