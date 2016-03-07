-- // add new write scopes
-- Migration SQL that makes the change goes here.

INSERT INTO groups (id, displayname) SELECT 'd2aff576-865d-473e-9f7f-3e016e0aea38', 'cloudbreak.networks'
    WHERE NOT EXISTS (SELECT 1 FROM groups WHERE displayName = 'cloudbreak.networks' AND identity_zone_id = 'uaa');
INSERT INTO groups (id, displayname) SELECT '68f8beb5-968b-4c74-b090-45b3191c7f78', 'cloudbreak.securitygroups'
    WHERE NOT EXISTS (SELECT 1 FROM groups WHERE displayName = 'cloudbreak.securitygroups' AND identity_zone_id = 'uaa');

INSERT INTO group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.networks'
    AND NOT EXISTS (SELECT 1 FROM group_membership WHERE group_ID = gr.id AND member_id = usr.id);
INSERT INTO group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.securitygroups'
    AND NOT EXISTS (SELECT 1 FROM group_membership WHERE group_ID = gr.id AND member_id = usr.id);

-- //@UNDO
-- SQL to undo the change goes here.

DELETE FROM group_membership WHERE group_id = (SELECT id FROM groups where displayName = 'cloudbreak.networks' AND identity_zone_id = 'uaa');
DELETE FROM group_membership WHERE group_id = (SELECT id FROM groups where displayName = 'cloudbreak.securitygroups' AND identity_zone_id = 'uaa');

DELETE FROM groups WHERE displayName = 'cloudbreak.networks' AND identity_zone_id = 'uaa';
DELETE FROM groups WHERE displayName = 'cloudbreak.securitygroups' AND identity_zone_id = 'uaa';


