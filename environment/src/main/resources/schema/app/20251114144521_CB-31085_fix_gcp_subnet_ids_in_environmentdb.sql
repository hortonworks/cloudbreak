-- // CB-31085 fix gcp subnet ids in environmentdb
-- Migration SQL that makes the change goes here.

UPDATE environment_network AS en
SET subnetmetas = (SELECT jsonb_object_agg(
                                  key,
                                  jsonb_set(value, '{id}', to_jsonb(value ->> 'name'))
                          )
                   FROM jsonb_each(en.subnetmetas::jsonb))
WHERE en.network_platform = 'GCP'
  AND EXISTS (SELECT 1
              FROM jsonb_each(en.subnetmetas::jsonb) AS subnet(key, value)
              WHERE subnet.value ->> 'id' IS DISTINCT FROM subnet.value ->> 'name');

-- //@UNDO
-- SQL to undo the change goes here.


