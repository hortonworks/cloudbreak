-- // CB-3962 – create validation for missing blueprint on cluster definition create
-- Migration SQL that makes the change goes here.
DELETE
FROM clustertemplate ct
WHERE ct."name" IN
    (SELECT ct.NAME
     FROM clustertemplate ct
     INNER JOIN stack s ON s.id = ct.stacktemplate_id
     INNER JOIN cluster c ON c.stack_id = s.id
     WHERE c.blueprint_id IS NULL)

-- //@UNDO
-- SQL to undo the change goes here.


