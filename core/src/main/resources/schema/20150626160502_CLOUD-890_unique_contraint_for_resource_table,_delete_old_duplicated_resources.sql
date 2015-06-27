-- // CLOUD-890 unique contraint for resource table, delete old duplicated resources
-- Migration SQL that makes the change goes here.

--Delete old duplicated resources
DELETE FROM resource
WHERE resource_stack IN (
    SELECT r1.resource_stack AS stack_id
    FROM resource r1, resource r2
    WHERE
      r1.resourcename=r2.resourcename AND
      r1.resourcetype=r2.resourcetype AND
      r1.resource_stack=r2.resource_stack AND
      r1.id<>r2.id
    GROUP BY r1.resourcename,r1.resourcetype,r1.resource_stack
);

ALTER TABLE ONLY resource ADD CONSTRAINT uk_namebytypebystack UNIQUE (resourcename, resourcetype, resource_stack);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ONLY resource DROP CONSTRAINT IF EXISTS uk_namebytypebystack;


