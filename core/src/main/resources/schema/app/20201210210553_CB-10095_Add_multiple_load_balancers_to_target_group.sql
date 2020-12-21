-- // CB-10095 Add multiple load balancers to target group
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS targetgroup_loadbalancer (
    targetgroupset_id     bigint NOT NULL,
    loadbalancerset_id    bigint NOT NULL
);

ALTER TABLE ONLY targetgroup_loadbalancer ADD CONSTRAINT targetgroup_loadbalancer_pkey PRIMARY KEY (targetgroupset_id, loadbalancerset_id);
ALTER TABLE ONLY targetgroup_loadbalancer ADD CONSTRAINT fk_targetgroup_loadbalancer_targetgroup_id FOREIGN KEY (targetgroupset_id) REFERENCES targetgroup(id);
ALTER TABLE ONLY targetgroup_loadbalancer ADD CONSTRAINT fk_targetgroup_loadbalancer_loadbalancer_id FOREIGN KEY (loadbalancerset_id) REFERENCES loadbalancer(id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS targetgroup_loadbalancer;