create table if not exists externalized_compute_cluster
(
    id                          bigserial                  primary key,
    accountid                   varchar(255)               not null,
    resourcecrn                 varchar(255)               not null,
    environmentcrn              text,
    name                        varchar(255)               not null,
    liftiename                  varchar(255),
    created                     bigint                     not null,
    deleted                     bigint,
    tags                        text                       not null
);

create unique index if not exists unq_index_ext_cluster_accountid_name_deleted_is_null
    on externalized_compute_cluster (accountid, name)
    where (deleted IS NULL);

create unique index if not exists unq_index_ext_cluster_accountid_name_deleted_is_not_null
    on externalized_compute_cluster (accountid, name, deleted)
    where (deleted IS NOT NULL);

-- //@UNDO
-- SQL to undo the change goes here.

drop table if exists externalized_compute_cluster;