-- // CLOUD-91820 [API] After upgrade time stamps are corrupted in event history
-- Migration SQL that makes the change goes here.

UPDATE structuredevent
SET structuredeventjson=(SELECT jsonb_set(structuredeventjson::jsonb, '{operation,timestamp}', (timestamp*1000)::text::jsonb)
                            FROM structuredevent
                            WHERE sub.id=id),
    timestamp=timestamp*1000::bigint
FROM (SELECT id FROM structuredevent
        WHERE timestamp < 1000000000000) AS sub
WHERE sub.id = structuredevent.id;

-- //@UNDO
-- SQL to undo the change goes here.


