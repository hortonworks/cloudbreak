-- // add new write scopes
-- Migration SQL that makes the change goes here.

--INSERT into groups (id, displayname) VALUES ('d2aff576-865d-473e-9f7f-3e016e0aea38', 'cloudbreak.networks');
--INSERT into groups (id, displayname) VALUES ('68f8beb5-968b-4c74-b090-45b3191c7f78', 'cloudbreak.securitygroups');

--INSERT into group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.networks';
--INSERT into group_membership SELECT gr.id AS group_id, usr.id AS member_id, 'USER', 'MEMBER' FROM users usr, groups gr WHERE gr.displayname='cloudbreak.securitygroups';

--INSERT into group_membership SELECT 'd2aff576-865d-473e-9f7f-3e016e0aea38' AS group_id, m.member_id, 'USER' AS member_type, 'MEMBER' as authorities from groups gr, group_membership m where gr.displayname='cloudbreak.templates' and m.group_id=gr.id;
--INSERT into group_membership SELECT '68f8beb5-968b-4c74-b090-45b3191c7f78' AS group_id, m.member_id, 'USER' AS member_type, 'MEMBER' as authorities from groups gr, group_membership m where gr.displayname='cloudbreak.templates' and m.group_id=gr.id;

-- //@UNDO
-- SQL to undo the change goes here.

--DELETE FROM group_membership WHERE group_id=(SELECT id FROM groups where displayName = 'cloudbreak.networks');
--DELETE FROM group_membership WHERE group_id=(SELECT id FROM groups where displayName = 'cloudbreak.securitygroups');

--DELETE FROM groups WHERE id='d2aff576-865d-473e-9f7f-3e016e0aea38';
--DELETE FROM groups WHERE id='68f8beb5-968b-4c74-b090-45b3191c7f78';


