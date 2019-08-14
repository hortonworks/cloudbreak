-- // CB-2576_admin_group_name

ALTER TABLE freeipa
    ADD COLUMN admin_group_name VARCHAR(255);

 UPDATE freeipa set admin_group_name='admins';

 -- //@UNDO

 ALTER TABLE freeipa
    DROP COLUMN IF EXISTS admin_group_name;