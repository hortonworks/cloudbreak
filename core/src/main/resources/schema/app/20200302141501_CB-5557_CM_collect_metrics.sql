-- // CB-5557 Collect CM related metrics
-- Migration SQL that makes the change goes here.

alter table cluster add COLUMN cloudbreakClusterManagerMonitoringUser varchar(255);
alter table cluster add COLUMN cloudbreakClusterManagerMonitoringPassword varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

alter table cluster drop COLUMN cloudbreakClusterManagerMonitoringUser;
alter table cluster drop COLUMN cloudbreakClusterManagerMonitoringPassword;
