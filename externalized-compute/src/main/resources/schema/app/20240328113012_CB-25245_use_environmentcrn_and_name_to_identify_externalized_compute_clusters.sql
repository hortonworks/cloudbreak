-- // CB-25245 use environmentCrn and name to identify externalized compute clusters
-- Migration SQL that makes the change goes here.

drop index if exists unq_index_ext_cluster_accountid_name_deleted_is_not_null;

drop index if exists unq_index_ext_cluster_accountid_name_deleted_is_null;

create unique index if not exists unq_index_ext_cluster_environmentcrn_name_deleted_is_null
    on externalized_compute_cluster (environmentcrn, name)
    where (deleted IS NULL);

alter table externalized_compute_cluster alter column environmentcrn set not null;

-- //@UNDO
-- SQL to undo the change goes here.

drop index if exists unq_index_ext_cluster_environmentcrn_name_deleted_is_null;

create unique index if not exists unq_index_ext_cluster_accountid_name_deleted_is_null
    on externalized_compute_cluster (accountid, name)
    where (deleted IS NULL);

alter table externalized_compute_cluster alter column environmentcrn drop not null;