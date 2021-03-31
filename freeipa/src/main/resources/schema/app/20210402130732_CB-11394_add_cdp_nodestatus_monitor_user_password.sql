-- // CB-11394 Store cdp nodestatus monitor user/password in stack
-- Migration SQL that makes the change goes here.

alter table stack add COLUMN IF NOT EXISTS cdpnodestatusmonitoruser text;
alter table stack add COLUMN IF NOT EXISTS cdpnodestatusmonitorpassword text;

-- //@UNDO
-- SQL to undo the change goes here.

alter table stack drop COLUMN IF EXISTS cdpnodestatusmonitoruser;
alter table stack drop COLUMN IF EXISTS cdpnodestatusmonitorpassword;