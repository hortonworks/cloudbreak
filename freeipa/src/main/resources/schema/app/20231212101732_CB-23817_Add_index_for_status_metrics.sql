-- // CB-23817 Add index for status metrics
-- Migration SQL that makes the change goes here.

create index if not exists stack_cloudplatform_terminated_stackstatusid_idx ON stack (cloudplatform, terminated, stackstatus_id);
create index if not exists stack_tunnel_terminated_stackstatusid_idx ON stack (tunnel, terminated, stackstatus_id);
create index if not exists stackstatus_id_status_idx ON stackstatus (id, status);

-- //@UNDO
-- SQL to undo the change goes here.

drop index if exists stack_cloudplatform_terminated_stackstatusid_idx;
drop index if exists stack_tunnel_terminated_stackstatusid_idx;
drop index if exists stackstatus_id_status_idx;