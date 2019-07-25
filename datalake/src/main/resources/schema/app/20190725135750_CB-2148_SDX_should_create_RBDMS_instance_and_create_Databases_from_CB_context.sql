-- // CB-2148 SDX should create RBDMS instance and create Databases from CB context
-- Migration SQL that makes the change goes here.

update sdxcluster set status='STACK_CREATION_IN_PROGRESS' where status='REQUESTED_FROM_CLOUDBREAK';

-- //@UNDO
-- SQL to undo the change goes here.

update sdxcluster set status='REQUESTED_FROM_CLOUDBREAK' where status='STACK_CREATION_IN_PROGRESS';

