-- // add new read scopes
-- Migration SQL that makes the change goes here.

--INSERT INTO groups (id, displayname) VALUES ('2a4f837e-2ab0-4663-8644-de23047d040d', 'cloudbreak.blueprints.read');
--INSERT into groups (id, displayname) VALUES ('9fc82f95-0318-4dda-af65-ea32e2f00497', 'cloudbreak.templates.read');
--INSERT into groups (id, displayname) VALUES ('8cbffdab-560d-4e14-9d1e-d5ff3f43bec6', 'cloudbreak.credentials.read');
--INSERT into groups (id, displayname) VALUES ('7f85eaad-4dac-4f74-9238-aef0e585df07', 'cloudbreak.recipes.read');
--INSERT into groups (id, displayname) VALUES ('aac2dad1-6a7b-4ae2-9c58-e71d367d0e2d', 'cloudbreak.networks.read');
--INSERT into groups (id, displayname) VALUES ('6674545b-295c-4f23-b1b4-953447dc5f40', 'cloudbreak.securitygroups.read');
--INSERT into groups (id, displayname) VALUES ('f07b098a-2700-471a-9f0d-f7dd730db95d', 'cloudbreak.stacks.read');

--INSERT into group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.blueprints.read';
--INSERT into group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.templates.read';
--INSERT into group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.credentials.read';
--INSERT into group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.recipes.read';
--INSERT into group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.networks.read';
--INSERT into group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.securitygroups.read';
--INSERT into group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.stacks.read';

-- //@UNDO
-- SQL to undo the change goes here.

--DELETE FROM groups WHERE id='2a4f837e-2ab0-4663-8644-de23047d040d';
--DELETE FROM groups WHERE id='9fc82f95-0318-4dda-af65-ea32e2f00497';
--DELETE FROM groups WHERE id='8cbffdab-560d-4e14-9d1e-d5ff3f43bec6';
--DELETE FROM groups WHERE id='7f85eaad-4dac-4f74-9238-aef0e585df07';
--DELETE FROM groups WHERE id='aac2dad1-6a7b-4ae2-9c58-e71d367d0e2d';
--DELETE FROM groups WHERE id='6674545b-295c-4f23-b1b4-953447dc5f40';
--DELETE FROM groups WHERE id='f07b098a-2700-471a-9f0d-f7dd730db95d';

--DELETE FROM group_membership WHERE group_id=(SELECT id FROM groups where displayName = 'cloudbreak.blueprints.read');
--DELETE FROM group_membership WHERE group_id=(SELECT id FROM groups where displayName = 'cloudbreak.templates.read');
--DELETE FROM group_membership WHERE group_id=(SELECT id FROM groups where displayName = 'cloudbreak.credentials.read');
--DELETE FROM group_membership WHERE group_id=(SELECT id FROM groups where displayName = 'cloudbreak.recipes.read');
--DELETE FROM group_membership WHERE group_id=(SELECT id FROM groups where displayName = 'cloudbreak.networks.read');
--DELETE FROM group_membership WHERE group_id=(SELECT id FROM groups where displayName = 'cloudbreak.securitygroups.read');
--DELETE FROM group_membership WHERE group_id=(SELECT id FROM groups where displayName = 'cloudbreak.stacks.read');
