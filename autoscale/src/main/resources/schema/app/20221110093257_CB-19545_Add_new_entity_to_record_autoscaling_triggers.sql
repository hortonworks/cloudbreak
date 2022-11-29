-- // CB-19545 Add new entity to record autoscaling triggers
-- Migration SQL that makes the change goes here.
create sequence if not exists scaling_trigger_id_seq;

create table if not exists scaling_trigger (
	id bigint default nextval('scaling_trigger_id_seq'::regclass) not null
	constraint scaling_trigger_pkey primary key,
	trigger_crn varchar(255),
	flow_id varchar(255),
	start_time timestamp,
	end_time timestamp,
	trigger_reason text,
	trigger_status varchar(255),
	cluster_id bigint,

	constraint fk_scaling_trigger_cluster_id foreign key (cluster_id) references cluster(id)
);

create unique index if not exists idx_trigger_crn on scaling_trigger(trigger_crn);

-- //@UNDO
-- SQL to undo the change goes here.

drop table if exists scaling_trigger;
drop sequence if exists scaling_trigger_id_seq;
