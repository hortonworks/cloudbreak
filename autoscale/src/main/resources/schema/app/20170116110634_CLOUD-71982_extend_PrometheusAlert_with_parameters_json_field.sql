-- // CLOUD-71982 extend PrometheusAlert with parameters json field
-- Migration SQL that makes the change goes here.

ALTER TABLE prometheusalert ADD COLUMN parameters TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE prometheusalert DROP COLUMN IF EXISTS parameters;


