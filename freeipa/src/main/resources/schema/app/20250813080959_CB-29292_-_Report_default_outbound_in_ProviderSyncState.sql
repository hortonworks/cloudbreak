-- // CB-29292 - Report default outbound in ProviderSyncState
-- Migration SQL that makes the change goes here.

-- The first UPDATE statement handles resources that have existing attributes and adds the "outboundType":"NOT_DEFINED" field to them.
-- The second UPDATE statement (newly added) handles resources where attributes are NULL, empty, or an empty JSON object. For these cases, it:
--    - Retrieves the attributes from the corresponding network table entry
--    - Uses a JOIN through the stack table to ensure we get the correct network for each resource
--    - Appends the "outboundType":"NOT_DEFINED" field to the retrieved network attributes
--    - Uses COALESCE to handle potential NULL values from the network table
--    - Has an EXISTS clause to ensure we only update resources that have a corresponding network entry                                                                                                                                                                                    This approach ensures that all Azure network resources will have the outboundType field, preserving any existing attribute data from either the resource record itself or from the related network record.

UPDATE resource
SET attributes = attributes::jsonb || '{"outboundType":"NOT_DEFINED"}'::jsonb
WHERE
    resourcetype = 'AZURE_NETWORK'
  AND attributes IS NOT NULL
  AND attributes != ''
  AND attributes != '{}'
  -- Only update if outboundType doesn't exist
  AND attributes::text NOT LIKE '%outboundType%';

-- For resources with empty or NULL attributes, copy from network table and append outboundType + attributeType fields.
UPDATE resource r
SET attributes = COALESCE(
                         (
                             SELECT n.attributes::jsonb
                             FROM network n
                                      JOIN stack s ON s.network_id = n.id
                             WHERE s.id = r.resource_stack
                         ),
                         '{}'::jsonb
                 ) || '{"outboundType":"NOT_DEFINED", "attributeType":"com.sequenceiq.cloudbreak.cloud.model.NetworkAttributes"}'::jsonb
WHERE
    resourcetype = 'AZURE_NETWORK'
  AND (attributes IS NULL OR attributes = '' OR attributes = '{}')
  AND EXISTS (
    SELECT 1
    FROM network n
             JOIN stack s ON s.network_id = n.id
    WHERE s.id = r.resource_stack
);


-- //@UNDO
-- SQL to undo the change goes here.
-- Remove the outboundType field that was added in the migration
UPDATE resource
SET attributes = attributes::jsonb - 'outboundType'
WHERE
    resourcetype = 'AZURE_NETWORK'
  AND attributes IS NOT NULL
  -- Only update records with the exact value we added
  AND attributes::text LIKE '%"outboundType":%"NOT_DEFINED"%';
