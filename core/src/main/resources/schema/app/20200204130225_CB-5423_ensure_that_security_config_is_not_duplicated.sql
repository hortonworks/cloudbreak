-- // CB-5423 ensure that security config is not duplicated
-- Migration SQL that makes the change goes here.

ALTER TABLE securityconfig
   ADD CONSTRAINT uk_securityconfig_stackid UNIQUE (stack_id);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE securityconfig
    DROP CONSTRAINT IF EXISTS uk_securityconfig_stackid;