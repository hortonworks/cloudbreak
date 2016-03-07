-- // add new write scopes
-- Migration SQL that makes the change goes here.

INSERT INTO groups (id, displayname) SELECT '1e0696ea-9250-405a-aaac-7596a8c15076', 'cloudbreak.sssdconfigs'
    WHERE NOT EXISTS (SELECT 1 FROM groups WHERE displayName = 'cloudbreak.sssdconfigs' AND identity_zone_id = 'uaa');
INSERT INTO groups (id, displayname) SELECT 'a1eb419b-0a1b-4bb7-b900-218a641ddac6', 'cloudbreak.sssdconfigs.read'
    WHERE NOT EXISTS (SELECT 1 FROM groups WHERE displayName = 'cloudbreak.sssdconfigs.read' AND identity_zone_id = 'uaa');

INSERT INTO group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.sssdconfigs'
    AND NOT EXISTS (SELECT 1 FROM group_membership WHERE group_ID = gr.id AND member_id = usr.id);
INSERT INTO group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.sssdconfigs.read'
        AND NOT EXISTS (SELECT 1 FROM group_membership WHERE group_ID = gr.id AND member_id = usr.id);

--UPDATE oauth_client_details SET scope = CONCAT(scope, ',cloudbreak.sssdconfigs,cloudbreak.sssdconfigs.read') WHERE client_id='uluwatu';
--UPDATE oauth_client_details SET scope = CONCAT(scope, ',cloudbreak.sssdconfigs,cloudbreak.sssdconfigs.read') WHERE client_id='cloudbreak_shell';

-- //@UNDO
-- SQL to undo the change goes here.

DELETE FROM group_membership WHERE group_id = (SELECT id FROM groups where displayName = 'cloudbreak.sssdconfigs' AND identity_zone_id = 'uaa');
DELETE FROM group_membership WHERE group_id = (SELECT id FROM groups where displayName = 'cloudbreak.sssdconfigs.read' AND identity_zone_id = 'uaa');

DELETE FROM groups WHERE displayName = 'cloudbreak.sssdconfigs' AND identity_zone_id = 'uaa';
DELETE FROM groups WHERE displayName = 'cloudbreak.sssdconfigs.read' AND identity_zone_id = 'uaa';

--UPDATE oauth_client_details SET scope = REPLACE(scope, ',cloudbreak.sssdconfigs,cloudbreak.sssdconfigs.read', '') WHERE client_id='uluwatu';
--UPDATE oauth_client_details SET scope = REPLACE(scope, ',cloudbreak.sssdconfigs,cloudbreak.sssdconfigs.read', '') WHERE client_id='cloudbreak_shell';