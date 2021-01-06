-- // CB-10095 Add multiple load balancers to target group
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS targetgroup_loadbalancer (
    targetgroupset_id     bigint NOT NULL,
    loadbalancerset_id    bigint NOT NULL
);


-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS targetgroup_loadbalancer;
