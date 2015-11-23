-- // CLOUD-47850 Refactor disk types
-- Migration SQL that makes the change goes here.

update template set gcprawdisktype='pd-ssd' where gcprawdisktype='SSD';
update template set gcprawdisktype='pd-standard' where gcprawdisktype='HDD';

update template set volumetype='standard' where volumetype='Standard';
update template set volumetype='ephemeral' where volumetype='Ephemeral';
update template set volumetype='gp2' where volumetype='Gp2';



-- //@UNDO
-- SQL to undo the change goes here.


update template set gcprawdisktype='SSD' where gcprawdisktype='pd-ssd';
update template set gcprawdisktype='HDD' where gcprawdisktype='pd-standard';

update template set volumetype='Standard' where volumetype='standard';
update template set volumetype='Ephemeral' where volumetype='ephemeral';
update template set volumetype='Gp2' where volumetype='gp2';
