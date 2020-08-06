-- // DISTX-527 cleanup unused tables
-- Migration SQL that makes the change goes here.
DROP TABLE IF EXISTS failed_node;

DROP TABLE IF EXISTS metricalert;

DROP TABLE IF EXISTS prometheusalert;

-- //@UNDO
-- SQL to undo the change goes here.


