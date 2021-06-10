-- // CB-12910 Populate rackId in InstanceMetaData core
-- Migration SQL that makes the change goes here.

ALTER TABLE instancemetadata
    ADD COLUMN IF NOT EXISTS availabilityzone VARCHAR(255),
    ADD COLUMN IF NOT EXISTS rackid TEXT;


WITH metadata_stack_temp AS (
    SELECT
        imd.id AS metadata_id,
        s.network_id AS stack_network_id
    FROM
        instancemetadata AS imd,
        instancegroup AS ig,
        stack AS s
    WHERE
          imd.instancegroup_id = ig.id
      AND
          ig.stack_id = s.id
), network_temp AS (
    SELECT
        id AS network_id,
        attributes::jsonb ->> 'subnetId' AS network_subnetid
    FROM
        network
)
UPDATE instancemetadata
SET
    subnetid = nt.network_subnetid
FROM
    metadata_stack_temp AS mst,
    network_temp AS nt
WHERE
      id = mst.metadata_id
  AND
      (subnetid IS NULL OR subnetid = '')
  AND
      mst.stack_network_id = nt.network_id;


WITH metadata_stack_temp AS (
    SELECT
           imd.id AS metadata_id,
           s.availabilityzone AS stack_availabilityzone
    FROM
         instancemetadata AS imd,
         instancegroup AS ig,
         stack AS s
    WHERE
          imd.instancegroup_id = ig.id
      AND
          ig.stack_id = s.id
)
UPDATE instancemetadata
SET
    availabilityzone = mst.stack_availabilityzone,
    rackid = concat('/',
        CASE WHEN (mst.stack_availabilityzone IS NULL OR mst.stack_availabilityzone = '')
            THEN (CASE WHEN (subnetid IS NULL OR subnetid = '')
                THEN 'default-rack'
                ELSE subnetid
                END)
            ELSE mst.stack_availabilityzone
            END)
FROM
     metadata_stack_temp AS mst
WHERE
      id = mst.metadata_id;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancemetadata
    DROP COLUMN IF EXISTS availabilityzone,
    DROP COLUMN IF EXISTS rackid;
