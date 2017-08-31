-- // CLOUD-86443 Need options with Cloudbreak to expand existing cluster into new subnet, current subnet has no new IPs available
-- Migration SQL that makes the change goes here.

ALTER TABLE instancemetadata ADD COLUMN subnetId VARCHAR(255);

UPDATE instancemetadata SET subnetId = subq.sid
FROM (select im.id as id, n.attributes::json->>'subnetId' as sid
        from instancemetadata im, instancegroup i, stack s, network n
        where im.instancegroup_id=i.id
        and i.stack_id=s.id
        and s.network_id=n.id
        and n.attributes is not null
        and n.cloudplatform='AZURE') as subq
WHERE instancemetadata.id = subq.id;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancemetadata DROP COLUMN IF EXISTS subnetId;
