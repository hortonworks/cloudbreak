-- // CB-2576_admin_group_name

ALTER TABLE environment
    ADD COLUMN admin_group_name VARCHAR(255);

 UPDATE environment set admin_group_name='admins';

 -- //@UNDO

 ALTER TABLE environment
    DROP COLUMN IF EXISTS admin_group_name;