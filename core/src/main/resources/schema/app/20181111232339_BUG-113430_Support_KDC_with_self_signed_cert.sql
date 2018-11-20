-- // BUG-113430_Support_KDC_with_self_signed_cert
-- Migration SQL that makes the change goes here.

ALTER TABLE kerberosconfig ADD COLUMN IF NOT EXISTS verifykdctrust BOOLEAN DEFAULT true;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS verifykdctrust;
