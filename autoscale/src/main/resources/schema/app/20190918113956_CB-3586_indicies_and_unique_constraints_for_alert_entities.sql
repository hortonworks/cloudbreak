-- // CB-3586 indicies and unique constraints for alert entities
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS metricalert_clusterid_id_idx ON metricalert(cluster_id, id);
CREATE INDEX IF NOT EXISTS timealert_clusterid_id_idx ON timealert(cluster_id, id);
CREATE INDEX IF NOT EXISTS prometheusalert_clusterid_id_idx ON prometheusalert(cluster_id, id);

DELETE FROM metricalert m1 USING metricalert m2 WHERE m1.id > m2.id AND m1.name = m2.name;
DELETE FROM timealert t1 USING timealert t2 WHERE t1.id > t2.id AND t1.name = t2.name;
DELETE FROM prometheusalert p1 USING prometheusalert p2 WHERE p1.id > p2.id AND p1.name = p2.name;

ALTER TABLE metricalert DROP CONSTRAINT IF EXISTS fk_metricalert_scalingpolicy_id;
ALTER TABLE timealert DROP CONSTRAINT IF EXISTS fk_timealert_scalingpolicy_id;
ALTER TABLE prometheusalert DROP CONSTRAINT IF EXISTS fk_prometheusalert_scalingpolicy_id;

DELETE FROM scalingpolicy sp1 USING scalingpolicy sp2 WHERE sp1.id > sp2.id AND sp1.name = sp2.name;
DELETE FROM metricalert WHERE scalingpolicy_id NOT IN (SELECT id from scalingpolicy);
DELETE FROM timealert WHERE scalingpolicy_id NOT IN (SELECT id from scalingpolicy);
DELETE FROM prometheusalert WHERE scalingpolicy_id NOT IN (SELECT id from scalingpolicy);

ALTER TABLE metricalert ADD CONSTRAINT fk_metricalert_scalingpolicy_id FOREIGN KEY (scalingpolicy_id) REFERENCES scalingpolicy(id);
ALTER TABLE timealert ADD CONSTRAINT fk_timealert_scalingpolicy_id FOREIGN KEY (scalingpolicy_id) REFERENCES scalingpolicy(id);
ALTER TABLE prometheusalert ADD CONSTRAINT fk_prometheusalert_scalingpolicy_id FOREIGN KEY (scalingpolicy_id) REFERENCES scalingpolicy(id);

ALTER TABLE metricalert DROP CONSTRAINT IF EXISTS uk_metricalert_clusterid_name;
ALTER TABLE timealert DROP CONSTRAINT IF EXISTS uk_timealert_clusterid_name;
ALTER TABLE prometheusalert DROP CONSTRAINT IF EXISTS uk_prometheusalert_clusterid_name;

ALTER TABLE metricalert ADD CONSTRAINT uk_metricalert_clusterid_name UNIQUE (cluster_id, name);
ALTER TABLE timealert ADD CONSTRAINT uk_timealert_clusterid_name UNIQUE (cluster_id, name);
ALTER TABLE prometheusalert ADD CONSTRAINT uk_prometheusalert_clusterid_name UNIQUE (cluster_id, name);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS metricalert_clusterid_id_idx;
DROP INDEX IF EXISTS timealert_clusterid_id_idx;
DROP INDEX IF EXISTS  prometheusalert_clusterid_id_idx;

ALTER TABLE metricalert DROP CONSTRAINT IF EXISTS uk_metricalert_clusterid_name;
ALTER TABLE timealert DROP CONSTRAINT IF EXISTS uk_timealert_clusterid_name;
ALTER TABLE prometheusalert DROP CONSTRAINT IF EXISTS uk_prometheusalert_clusterid_name;
