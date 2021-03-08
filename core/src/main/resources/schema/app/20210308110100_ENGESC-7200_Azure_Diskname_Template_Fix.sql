-- // ENGESC-7200 Azure issue with disk naming fix for existing stacks
-- Migration SQL that makes the change goes here.

UPDATE component c
SET attributes = regexp_replace(attributes,
'''datadisk'',\s*''\$\{instance.instanceId}'',\s*''\$\{volume_index}''',
'''datadisk'', ''${instance.instanceId}'', ''-'', ''${volume_index}''', 'g')
FROM stack s
WHERE c.stack_id = s.id
AND c.componenttype = 'STACK_TEMPLATE'
AND s.terminated IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.
