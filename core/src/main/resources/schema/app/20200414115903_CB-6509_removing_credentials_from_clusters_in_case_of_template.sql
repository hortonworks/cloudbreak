-- // CB-6509 removing credentials from clusters in case of template
-- Migration SQL that makes the change goes here.
UPDATE cluster
SET
 username = null,
 password = null,
 cloudbreakambariuser= null,
 cloudbreakambaripassword = null,
 dpambariuser = null,
 dpambaripassword = null,
 dpclustermanageruser = null,
 dpclustermanagerpassword =null,
 cloudbreakclustermanageruser = null,
 cloudbreakclustermanagerpassword = null,
 cloudbreakclustermanagermonitoringuser = null,
 cloudbreakclustermanagermonitoringpassword = null
WHERE id in (
      SELECT cluster.id FROM cluster, stack WHERE stack.type = 'TEMPLATE' and stack.id = cluster.stack_id
);


-- //@UNDO
-- SQL to undo the change goes here.


