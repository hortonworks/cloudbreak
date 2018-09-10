-- // BUG-110749 Invalid description and permissions for workspace (still org)
-- Migration SQL that makes the change goes here.

UPDATE workspace SET description = 'Default workspace for the user.'
WHERE description = 'Default organization for the user.';

UPDATE user_workspace_permissions SET permissions = '["ALL:WRITE","ALL:READ","WORKSPACE:MANAGE"]';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE user_workspace_permissions SET permissions = '["ALL:WRITE","ALL:READ","ORG:MANAGE"]';

UPDATE workspace SET description = 'Default organization for the user.'
WHERE description = 'Default workspace for the user.';

