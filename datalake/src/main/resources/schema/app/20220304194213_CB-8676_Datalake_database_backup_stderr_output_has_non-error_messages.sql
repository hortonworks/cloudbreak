-- // CB-8676 Datalake database backup stderr output has non-error messages
-- Migration SQL that makes the change goes here.

alter table sdxoperation alter column statusreason type character varying(2000);

-- //@UNDO
-- SQL to undo the change goes here.

alter table sdxoperation alter column statusreason type character varying(255);
