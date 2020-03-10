-- // cb-5734-add-loadbased-alert-type
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS loadalert (
    id bigint NOT NULL,
    description character varying(255),
    name character varying(255),
    load_alert_config character varying(1024),
    scalingpolicy_id bigint,
    cluster_id bigint
);

ALTER TABLE loadalert
    ADD CONSTRAINT loadalert_pkey PRIMARY KEY (id);

ALTER TABLE loadalert
    ADD CONSTRAINT fk_loadalert_cluster_id FOREIGN KEY (cluster_id) REFERENCES cluster(id);

ALTER TABLE loadalert
    ADD CONSTRAINT fk_loadalert_scalingpolicy_id FOREIGN KEY (scalingpolicy_id) REFERENCES scalingpolicy(id);


-- //@UNDO
-- SQL to undo the change goes here.


DROP TABLE IF EXISTS loadalert;