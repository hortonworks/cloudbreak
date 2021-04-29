-- // CB-12302 Make edge node templates default
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET status='DEFAULT' WHERE name='7.2.2 COD Edge Node';
UPDATE blueprint SET status='DEFAULT' WHERE name='7.2.3 COD Edge Node';
UPDATE blueprint SET status='DEFAULT' WHERE name='7.2.6 COD Edge Node';
UPDATE blueprint SET status='DEFAULT' WHERE name='7.2.7 COD Edge Node';
UPDATE blueprint SET status='DEFAULT' WHERE name='7.2.8 COD Edge Node';
UPDATE blueprint SET status='DEFAULT' WHERE name='7.2.9 COD Edge Node';
UPDATE blueprint SET status='DEFAULT' WHERE name='7.2.10 COD Edge Node';

-- //@UNDO
-- SQL to undo the change goes here.
UPDATE blueprint SET status='USER_MANAGED' WHERE name='7.2.2 COD Edge Node';
UPDATE blueprint SET status='USER_MANAGED' WHERE name='7.2.3 COD Edge Node';
UPDATE blueprint SET status='USER_MANAGED' WHERE name='7.2.6 COD Edge Node';
UPDATE blueprint SET status='USER_MANAGED' WHERE name='7.2.7 COD Edge Node';
UPDATE blueprint SET status='USER_MANAGED' WHERE name='7.2.8 COD Edge Node';
UPDATE blueprint SET status='USER_MANAGED' WHERE name='7.2.9 COD Edge Node';
UPDATE blueprint SET status='USER_MANAGED' WHERE name='7.2.10 COD Edge Node';
