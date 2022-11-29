-- // CB-19545 Add new entity to record autoscaling triggers
-- Migration SQL that makes the change goes here.
create sequence if not exists scaling_activity_id_seq;

create table if not exists scaling_activity (
	id bigint default nextval('scaling_activity_id_seq'::regclass) not null
	constraint scaling_activity_pkey primary key,
	activity_crn varchar(255),
	flow_id varchar(255),
	start_time timestamp,
	end_time timestamp,
	activity_reason text,
	activity_status varchar(255),
	cluster_id bigint,

	constraint fk_scaling_activity_cluster_id foreign key (cluster_id) references cluster(id)
);

create unique index if not exists idx_activity_crn on scaling_activity(activity_crn);

create index if not exists idx_start_time_activity_status on scaling_activity(start_time, activity_status);

create index if not exists idx_scaling_activity_cluster_id on scaling_activity(cluster_id);

-- //@UNDO
-- SQL to undo the change goes here.

drop table if exists scaling_activity;
drop sequence if exists scaling_activity_id_seq;
