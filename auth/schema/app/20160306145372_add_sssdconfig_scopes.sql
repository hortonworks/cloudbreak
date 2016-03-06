-- // add new write scopes
-- Migration SQL that makes the change goes here.

INSERT INTO groups (id, displayname) VALUES ('1e0696ea-9250-405a-aaac-7596a8c15076', 'cloudbreak.sssdconfigs');
INSERT INTO groups (id, displayname) VALUES ('a1eb419b-0a1b-4bb7-b900-218a641ddac6', 'cloudbreak.sssdconfigs.read');

INSERT INTO group_membership SELECT '1e0696ea-9250-405a-aaac-7596a8c15076' AS group_id, id, 'USER' AS member_type, 'MEMBER' as authorities from users;
INSERT INTO group_membership SELECT 'a1eb419b-0a1b-4bb7-b900-218a641ddac6' AS group_id, id, 'USER' AS member_type, 'MEMBER' as authorities from users;

UPDATE oauth_client_details SET scope = CONCAT(scope, ',cloudbreak.sssdconfigs,cloudbreak.sssdconfigs.read') WHERE client_id='uluwatu';
UPDATE oauth_client_details SET scope = CONCAT(scope, ',cloudbreak.sssdconfigs,cloudbreak.sssdconfigs.read') WHERE client_id='cloudbreak_shell';

-- //@UNDO
-- SQL to undo the change goes here.

DELETE FROM group_membership WHERE group_id='1e0696ea-9250-405a-aaac-7596a8c15076';
DELETE FROM group_membership WHERE group_id='a1eb419b-0a1b-4bb7-b900-218a641ddac6';

DELETE FROM groups WHERE id='1e0696ea-9250-405a-aaac-7596a8c15076';
DELETE FROM groups WHERE id='a1eb419b-0a1b-4bb7-b900-218a641ddac6';

UPDATE oauth_client_details SET scope = REPLACE(scope, ',cloudbreak.sssdconfigs,cloudbreak.sssdconfigs.read', '') WHERE client_id='uluwatu';
UPDATE oauth_client_details SET scope = REPLACE(scope, ',cloudbreak.sssdconfigs,cloudbreak.sssdconfigs.read', '') WHERE client_id='cloudbreak_shell';