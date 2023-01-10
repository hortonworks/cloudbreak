-- // DISTX-739 Fix EnableLoadBalancer set to false irrespective of true in custom cluster definition
-- Migration SQL that makes the change goes here.

ALTER TABLE clustertemplate ADD COLUMN IF NOT EXISTS enableloadbalancer BOOLEAN NOT NULL DEFAULT true;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE clustertemplate DROP COLUMN IF EXISTS enableloadbalancer;
