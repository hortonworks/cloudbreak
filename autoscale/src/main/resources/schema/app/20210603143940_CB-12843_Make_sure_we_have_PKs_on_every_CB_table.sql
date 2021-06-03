-- // CB-12843 Make sure we have PKs on every CB table
-- Migration SQL that makes the change goes here.

alter table subscription drop constraint if exists subscription_pkey;
alter table subscription add primary key (id);

-- //@UNDO
-- SQL to undo the change goes here.

alter table subscription drop constraint if exists subscription_pkey;
