-- // CB-10095 Add multiple load balancers to target group
-- Migration SQL that makes the change goes here.



-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS targetgroup_loadbalancer;
