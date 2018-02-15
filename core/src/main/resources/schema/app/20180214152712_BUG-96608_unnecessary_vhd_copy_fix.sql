-- // BUG-96608 unnecessary vhd copy fix
-- Migration SQL that makes the change goes here.

INSERT INTO stack_parameters (stack_id, value, key) SELECT stack.id, 'true', 'legacyImageStore' from stack WHERE cloudplatform = 'AZURE';

UPDATE template SET attributes='{"managedDisk":false}' WHERE cloudplatform = 'AZURE' AND attributes is null;

UPDATE template
SET attributes=(SELECT jsonb_set(attributes::jsonb, '{managedDisk}', 'false')
                FROM template
                WHERE sub.id=id)
FROM (SELECT id FROM template WHERE attributes::json ->> 'managedDisk' is null AND cloudplatform = 'AZURE') AS sub
WHERE sub.id = template.id;

-- //@UNDO
-- SQL to undo the change goes here.

DELETE FROM stack_parameters WHERE stack_parameters.key = 'legacyImageStore';