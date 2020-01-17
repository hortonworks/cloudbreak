-- // CB-4600 rename deprected blueprints
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET name=REPLACE(name, ' deprecated', CONCAT(' ',stackversion)) WHERE name LIKE '% deprecated%';

-- //@UNDO
-- SQL to undo the change goes here.


