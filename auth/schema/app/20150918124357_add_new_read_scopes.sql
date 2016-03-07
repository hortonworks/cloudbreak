-- // add new read scopes
-- Migration SQL that makes the change goes here.

INSERT INTO groups (id, displayname) SELECT '2a4f837e-2ab0-4663-8644-de23047d040d', 'cloudbreak.blueprints.read'
    WHERE NOT EXISTS (SELECT 1 FROM groups WHERE displayName = 'cloudbreak.blueprints.read' AND identity_zone_id = 'uaa');
INSERT INTO groups (id, displayname) SELECT '9fc82f95-0318-4dda-af65-ea32e2f00497', 'cloudbreak.templates.read'
    WHERE NOT EXISTS (SELECT 1 FROM groups WHERE displayName = 'cloudbreak.templates.read' AND identity_zone_id = 'uaa');
INSERT INTO groups (id, displayname) SELECT '8cbffdab-560d-4e14-9d1e-d5ff3f43bec6', 'cloudbreak.credentials.read'
    WHERE NOT EXISTS (SELECT 1 FROM groups WHERE displayName = 'cloudbreak.credentials.read' AND identity_zone_id = 'uaa');
INSERT INTO groups (id, displayname) SELECT '7f85eaad-4dac-4f74-9238-aef0e585df07', 'cloudbreak.recipes.read'
    WHERE NOT EXISTS (SELECT 1 FROM groups WHERE displayName = 'cloudbreak.recipes.read' AND identity_zone_id = 'uaa');
INSERT INTO groups (id, displayname) SELECT 'aac2dad1-6a7b-4ae2-9c58-e71d367d0e2d', 'cloudbreak.networks.read'
    WHERE NOT EXISTS (SELECT 1 FROM groups WHERE displayName = 'cloudbreak.networks.read' AND identity_zone_id = 'uaa');
INSERT INTO groups (id, displayname) SELECT '6674545b-295c-4f23-b1b4-953447dc5f40', 'cloudbreak.securitygroups.read'
    WHERE NOT EXISTS (SELECT 1 FROM groups WHERE displayName = 'cloudbreak.securitygroups.read' AND identity_zone_id = 'uaa');
INSERT INTO groups (id, displayname) SELECT 'f07b098a-2700-471a-9f0d-f7dd730db95d', 'cloudbreak.stacks.read'
    WHERE NOT EXISTS (SELECT 1 FROM groups WHERE displayName = 'cloudbreak.stacks.read' AND identity_zone_id = 'uaa');

INSERT INTO group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.blueprints.read'
    AND NOT EXISTS (SELECT 1 FROM group_membership WHERE group_id = gr.id AND member_id = member_id);
INSERT INTO group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.templates.read'
    AND NOT EXISTS (SELECT 1 FROM group_membership WHERE group_id = gr.id AND member_id = member_id);
INSERT INTO group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.credentials.read'
    AND NOT EXISTS (SELECT 1 FROM group_membership WHERE group_id = gr.id AND member_id = member_id);
INSERT INTO group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.recipes.read'
    AND NOT EXISTS (SELECT 1 FROM group_membership WHERE group_id = gr.id AND member_id = member_id);
INSERT INTO group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.networks.read'
    AND NOT EXISTS (SELECT 1 FROM group_membership WHERE group_id = gr.id AND member_id = member_id);
INSERT INTO group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.securitygroups.read'
    AND NOT EXISTS (SELECT 1 FROM group_membership WHERE group_id = gr.id AND member_id = member_id);
INSERT INTO group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.stacks.read'
    AND NOT EXISTS (SELECT 1 FROM group_membership WHERE group_id = gr.id AND member_id = member_id);

-- //@UNDO
-- SQL to undo the change goes here.

DELETE FROM groups WHERE displayName = 'cloudbreak.blueprints.read' AND identity_zone_id = 'uaa';
DELETE FROM groups WHERE displayName = 'cloudbreak.templates.read' AND identity_zone_id = 'uaa';
DELETE FROM groups WHERE displayName = 'cloudbreak.credentials.read' AND identity_zone_id = 'uaa';
DELETE FROM groups WHERE displayName = 'cloudbreak.recipes.read' AND identity_zone_id = 'uaa';
DELETE FROM groups WHERE displayName = 'cloudbreak.networks.read' AND identity_zone_id = 'uaa';
DELETE FROM groups WHERE displayName = 'cloudbreak.securitygroups.read' AND identity_zone_id = 'uaa';
DELETE FROM groups WHERE displayName = 'cloudbreak.stacks.read' AND identity_zone_id = 'uaa';

DELETE FROM group_membership WHERE group_id = (SELECT id FROM groups where displayName = 'cloudbreak.blueprints.read' AND identity_zone_id = 'uaa');
DELETE FROM group_membership WHERE group_id = (SELECT id FROM groups where displayName = 'cloudbreak.templates.read' AND identity_zone_id = 'uaa');
DELETE FROM group_membership WHERE group_id = (SELECT id FROM groups where displayName = 'cloudbreak.credentials.read' AND identity_zone_id = 'uaa');
DELETE FROM group_membership WHERE group_id = (SELECT id FROM groups where displayName = 'cloudbreak.recipes.read' AND identity_zone_id = 'uaa');
DELETE FROM group_membership WHERE group_id = (SELECT id FROM groups where displayName = 'cloudbreak.networks.read' AND identity_zone_id = 'uaa');
DELETE FROM group_membership WHERE group_id = (SELECT id FROM groups where displayName = 'cloudbreak.securitygroups.read' AND identity_zone_id = 'uaa');
DELETE FROM group_membership WHERE group_id = (SELECT id FROM groups where displayName = 'cloudbreak.stacks.read' AND identity_zone_id = 'uaa');