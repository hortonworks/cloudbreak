-- // CLOUD-70541 create prometheus alert table
-- Migration SQL that makes the change goes here.

CREATE TABLE prometheusalert (
    id bigint NOT NULL,
    description character varying(255),
    name character varying(255),
    scalingpolicy_id bigint,
    alert_state character varying(255),
    alert_rule text,
    period integer NOT NULL,
    cluster_id bigint
);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS prometheusalert;
