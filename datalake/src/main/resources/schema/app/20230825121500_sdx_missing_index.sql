-- // CB-22958 fix sdx unique index
-- Migration SQL that makes the change goes here

drop index if exists unq_index_sdxcluster_accid_envcrn_detached_deleted_is_null;

create unique index unq_index_sdxcluster_accid_envcrn_detached_deleted_is_null
    on public.sdxcluster (accountid, envcrn, (deleted IS NULL), detached)
    where (deleted IS NULL);

