-- // CB-10579 Clean up unused targetgroup loadbalancer columns
-- Migration SQL that makes the change goes here.

DROP INDEX IF EXISTS targetgroup_loadbalancer_id;
ALTER TABLE targetgroup DROP COLUMN IF EXISTS loadbalancer_id;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE targetgroup ADD COLUMN IF NOT EXISTS loadbalancer_id bigint;

ALTER TABLE ONLY targetgroup
    ADD CONSTRAINT fk_targetgroup_loadbalancer_id FOREIGN KEY (loadbalancer_id) REFERENCES loadbalancer(id);

UPDATE targetgroup SET loadbalancer_id =
    (SELECT targetgroup_loadbalancer.loadbalancerset_id
    FROM targetgroup_loadbalancer
    WHERE targetgroup_loadbalancer.targetgroupset_id = targetgroup.id limit 1);

CREATE INDEX IF NOT EXISTS targetgroup_loadbalancer_id ON targetgroup (loadbalancer_id);
