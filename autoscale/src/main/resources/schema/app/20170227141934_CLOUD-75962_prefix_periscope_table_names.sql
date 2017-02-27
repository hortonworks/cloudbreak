-- // CLOUD-75962_prefix_periscope_table_names
-- Migration SQL that makes the change goes here.

ALTER TABLE ambari RENAME TO as_ambari;
ALTER TABLE cluster RENAME TO as_cluster;
ALTER TABLE history RENAME TO as_history;
ALTER TABLE history_properties RENAME TO as_history_properties;
ALTER TABLE metricalert RENAME TO as_metricalert;
ALTER TABLE notification RENAME TO as_notification;
ALTER TABLE periscope_user RENAME TO as_periscope_user;
ALTER TABLE prometheusalert RENAME TO as_prometheusalert;
ALTER TABLE scalingpolicy RENAME TO as_scalingpolicy;
ALTER TABLE securityconfig RENAME TO as_securityconfig;
ALTER TABLE subscription RENAME TO as_subscription;
ALTER TABLE timealert RENAME TO as_timealert;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE as_ambari RENAME TO ambari;
ALTER TABLE as_cluster RENAME TO cluster;
ALTER TABLE as_history RENAME TO history;
ALTER TABLE as_history_properties RENAME TO history_properties;
ALTER TABLE as_metricalert RENAME TO metricalert;
ALTER TABLE as_notification RENAME TO notification;
ALTER TABLE as_periscope_user RENAME TO periscope_user;
ALTER TABLE as_prometheusalert RENAME TO prometheusalert;
ALTER TABLE as_scalingpolicy RENAME TO scalingpolicy;
ALTER TABLE as_securityconfig RENAME TO securityconfig;
ALTER TABLE as_subscription RENAME TO subscription;
ALTER TABLE as_timealert RENAME TO timealert;