-- // add new write scopes
-- Migration SQL that makes the change goes here.

INSERT into groups (id, displayname) VALUES ('2755f3e1-71af-45f3-88ae-f97836f03ef6', 'cloudbreak.sssdconfigs');
INSERT INTO groups (id, displayname) VALUES ('ba0f7ace-f4b0-45d8-a4d9-965ce52e9360', 'cloudbreak.sssdconfigs.read');

INSERT into group_membership SELECT '2755f3e1-71af-45f3-88ae-f97836f03ef6' AS group_id, id, 'USER' AS member_type, 'MEMBER' as authorities from users;
INSERT into group_membership SELECT 'ba0f7ace-f4b0-45d8-a4d9-965ce52e9360' AS group_id, id, 'USER' AS member_type, 'MEMBER' as authorities from users;

UPDATE oauth_client_details SET scope = CONCAT(scope, ',cloudbreak.sssdconfigs,cloudbreak.sssdconfigs.read') WHERE client_id='uluwatu';
UPDATE oauth_client_details SET scope = CONCAT(scope, ',cloudbreak.sssdconfigs,cloudbreak.sssdconfigs.read') WHERE client_id='cloudbreak_shell';

-- //@UNDO
-- SQL to undo the change goes here.

DELETE FROM group_membership WHERE group_id='2755f3e1-71af-45f3-88ae-f97836f03ef6';
DELETE FROM group_membership WHERE group_id='ba0f7ace-f4b0-45d8-a4d9-965ce52e9360';

DELETE FROM groups WHERE id='2755f3e1-71af-45f3-88ae-f97836f03ef6';
DELETE FROM groups WHERE id='ba0f7ace-f4b0-45d8-a4d9-965ce52e9360';

UPDATE oauth_client_details SET scope = REPLACE(scope, ',cloudbreak.sssdconfigs,cloudbreak.sssdconfigs.read', '') WHERE client_id='uluwatu';
UPDATE oauth_client_details SET scope = REPLACE(scope, ',cloudbreak.sssdconfigs,cloudbreak.sssdconfigs.read', '') WHERE client_id='cloudbreak_shell';
