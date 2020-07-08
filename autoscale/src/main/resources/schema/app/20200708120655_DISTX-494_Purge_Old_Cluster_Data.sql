-- // DISTX-494 Purge Old Cluster Data
-- Migration SQL that makes the change goes here.
-- Purge existing periscope cluster data, new cluster data would be auto-synced from CB.

delete from failed_node;
delete from history_properties;
delete from history;
delete from prometheusalert;
delete from loadalert;
delete from timealert;
delete from metricalert;
delete from scalingpolicy;
delete from securityconfig;
delete from cluster;
delete from cluster_manager;
delete from clusterpertain;


-- //@UNDO
-- SQL to undo the change goes here.


