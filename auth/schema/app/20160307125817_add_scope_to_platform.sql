-- // add scope to topology/platform
-- Migration SQL that makes the change goes here.

INSERT INTO groups (id, displayname) SELECT '9dd281cc-6aac-491f-8709-4f9fdc19cc87', 'cloudbreak.platforms'
    WHERE NOT EXISTS (SELECT 1 FROM groups WHERE displayName = 'cloudbreak.platforms' AND identity_zone_id = 'uaa');
INSERT INTO groups (id, displayname) SELECT '0be50bbf-9ef1-49a1-8ae4-daac8c0c6eab', 'cloudbreak.platforms.read'
    WHERE NOT EXISTS (SELECT 1 FROM groups WHERE displayName = 'cloudbreak.platforms.read' AND identity_zone_id = 'uaa');

INSERT INTO group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.platforms'
    AND NOT EXISTS (SELECT 1 FROM group_membership WHERE group_ID = gr.id AND member_id = usr.id);
INSERT INTO group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.platforms.read'
        AND NOT EXISTS (SELECT 1 FROM group_membership WHERE group_ID = gr.id AND member_id = usr.id);

--UPDATE oauth_client_details SET scope = CONCAT(scope, ',cloudbreak.platforms,cloudbreak.platforms.read') WHERE client_id='uluwatu';
--UPDATE oauth_client_details SET scope = CONCAT(scope, ',cloudbreak.platforms,cloudbreak.platforms.read') WHERE client_id='cloudbreak_shell';

-- //@UNDO
-- SQL to undo the change goes here.

DELETE FROM group_membership WHERE group_id = (SELECT id FROM groups where displayName = 'cloudbreak.platforms' AND identity_zone_id = 'uaa');
DELETE FROM group_membership WHERE group_id = (SELECT id FROM groups where displayName = 'cloudbreak.platforms.read' AND identity_zone_id = 'uaa');

DELETE FROM groups WHERE displayName = 'cloudbreak.platforms' AND identity_zone_id = 'uaa';
DELETE FROM groups WHERE displayName = 'cloudbreak.platforms.read' AND identity_zone_id = 'uaa';

--UPDATE oauth_client_details SET scope = REPLACE(scope, ',cloudbreak.platforms,cloudbreak.platforms.read', '') WHERE client_id='uluwatu';
--UPDATE oauth_client_details SET scope = REPLACE(scope, ',cloudbreak.platforms,cloudbreak.platforms.read', '') WHERE client_id='cloudbreak_shell';