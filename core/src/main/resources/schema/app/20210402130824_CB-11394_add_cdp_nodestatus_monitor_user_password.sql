-- // CB-11394 Store cdp nodestatus monitor user/password in cluster
-- Migration SQL that makes the change goes here.

alter table cluster add COLUMN cdpnodestatusmonitoruser text;
alter table cluster add COLUMN cdpnodestatusmonitorpassword text;

-- //@UNDO
-- SQL to undo the change goes here.

alter table cluster drop COLUMN cdpnodestatusmonitoruser;
alter table cluster drop COLUMN cdpnodestatusmonitorpassword;
