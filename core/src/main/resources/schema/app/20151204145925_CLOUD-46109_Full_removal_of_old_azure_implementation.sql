-- // CLOUD-46109 Full removal of old azure implementation
-- Migration SQL that makes the change goes here.

update template set cloudplatform='AZURE_RM' where cloudplatform='AZURE';
update network set cloudplatform='AZURE_RM' where cloudplatform='AZURE';

delete from hostgroup_recipe where hostgroup_id in (select h.id from credential c, stack s, instancegroup g, hostgroup h where s.credential_id = c.id and g.stack_id = s.id and h.instancegroup_id = g.id and c.dtype='AzureCredential');
delete from hostmetadata where hostgroup_id in (select h.id from credential c, stack s, instancegroup g, hostgroup h where s.credential_id = c.id and g.stack_id = s.id and h.instancegroup_id = g.id and c.dtype='AzureCredential');
delete from hostgroup where id in (select h.id from credential c, stack s, instancegroup g, hostgroup h where s.credential_id = c.id and g.stack_id = s.id and h.instancegroup_id = g.id and c.dtype='AzureCredential');
delete from instancemetadata where instancegroup_id in (select i.id from credential c, stack s, instancegroup i where s.credential_id = c.id and i.stack_id = s.id and c.dtype='AzureCredential');
delete from instancegroup where id in (select i.id from credential c, stack s, instancegroup i where s.credential_id = c.id and i.stack_id = s.id and c.dtype='AzureCredential');
delete from cluster where id in (select cl.id from credential c, stack s, cluster cl where s.credential_id = c.id and cl.stack_id = s.id and c.dtype='AzureCredential');
delete from resource where resourcetype in ('AZURE_VIRTUAL_MACHINE', 'AZURE_CLOUD_SERVICE', 'AZURE_RESERVED_IP', 'AZURE_BLOB', 'AZURE_STORAGE', 'AZURE_NETWORK', 'AZURE_AFFINITY_GROUP', 'AZURE_SERVICE_CERTIFICATE');
delete from stack_parameters where stack_id in (select s.id from credential c, stack s where s.credential_id = c.id and c.dtype='AzureCredential');
delete from securityconfig where stack_id in (select s.id from credential c, stack s where s.credential_id = c.id and c.dtype='AzureCredential');
delete from component where stack_id in (select s.id from credential c, stack s where s.credential_id = c.id and c.dtype='AzureCredential');
delete from stack where id in (select s.id from credential c, stack s where s.credential_id = c.id and c.dtype='AzureCredential');
delete from credential where dtype='AzureCredential';

-- //@UNDO
-- SQL to undo the change goes here.


