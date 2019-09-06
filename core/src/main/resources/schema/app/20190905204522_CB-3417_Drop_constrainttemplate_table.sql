-- // CB-3417 Drop constrainttemplate table
-- Migration SQL that makes the change goes here.

alter table hostgroup_constraint drop constraint fk_hostgroup_constraint_constrainttemplate_id;

alter table hostgroup_constraint drop column constrainttemplate_id;

drop table constrainttemplate;

drop sequence IF EXISTS constrainttemplate_id_seq;

-- //@UNDO
-- SQL to undo the change goes here.

create table constrainttemplate
(
    id bigint not null
        constraint constrainttemplate_pkey
            primary key,
    name varchar(255) not null,
    account varchar(255),
    deleted boolean not null,
    description text,
    owner varchar(255),
    publicinaccount boolean default false,
    cpu double precision not null,
    memory double precision not null,
    disk double precision not null,
    status varchar(255) default 'CREATED'::character varying not null,
    orchestratortype varchar(255) not null,
    workspace_id bigint
        constraint fk_constrainttemplate_organization
            references workspace,
    constraint constrainttemplatename_in_org_unique
        unique (name, workspace_id)
);

CREATE SEQUENCE IF NOT EXISTS constrainttemplate_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

create unique index constrainttemplate_id_idx
    on constrainttemplate (id);

create index constrainttemplate_name_idx
    on constrainttemplate (name);

create index constrainttemplate_org_id_idx
    on constrainttemplate (workspace_id);

ALTER TABLE hostgroup_constraint
    ADD COLUMN constrainttemplate_id BIGINT;

alter table hostgroup_constraint
    add constraint fk_hostgroup_constraint_constrainttemplate_id
        foreign key (constrainttemplate_id) references constrainttemplate;


