-- // CB-3417 drop constraint
-- Migration SQL that makes the change goes here.

alter table hostgroup add column if not exists instancegroup_id bigint;

update hostgroup h set instancegroup_id = (select hc.instancegroup_id from hostgroup_constraint hc where h.constraint_id = hc.id) where h.instancegroup_id is null;

alter table hostgroup
    add constraint hostgroup_instancegroup_fk
        foreign key (instancegroup_id) references instancegroup (id);

alter table hostgroup drop constraint fk_hostgroup_hostgroup_constraint_id;

alter table hostgroup drop column if exists constraint_id;

drop table hostgroup_constraint;

drop sequence if exists hostgroup_constraint_id_seq;

-- //@UNDO
-- SQL to undo the change goes here.

create table if not exists hostgroup_constraint
(
    id bigint not null
        constraint hostgroup_constraint_pkey
            primary key,
    instancegroup_id bigint
        constraint fk_hostgroup_constraint_instancegroup_id
            references instancegroup
);

create sequence if not exists hostgroup_constraint_id_seq;

alter table hostgroup add column if not exists constraint_id bigint;

insert into hostgroup_constraint (id, instancegroup_id) SELECT nextval('hostgroup_constraint_id_seq'), hg.instancegroup_id from hostgroup hg;

update hostgroup hg set constraint_id = (select hc.id from hostgroup_constraint hc where hc.instancegroup_id = hg.instancegroup_id);

alter table hostgroup drop constraint hostgroup_instancegroup_fk;

alter table hostgroup drop column if exists instancegroup_id;

alter table hostgroup add constraint fk_hostgroup_hostgroup_constraint_id
    foreign key (constraint_id) references hostgroup_constraint (id);
