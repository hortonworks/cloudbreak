-- // DISTX-399 Datahub Autoscaling Changes
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS loadalert (
    id bigint NOT NULL,
    description character varying(255),
    name character varying(255),
    load_alert_config text,
    scalingpolicy_id bigint,
    cluster_id bigint,
    CONSTRAINT loadalert_pkey PRIMARY KEY (id),
    CONSTRAINT fk_loadalert_cluster_id FOREIGN KEY (cluster_id) REFERENCES cluster(id),
    CONSTRAINT fk_loadalert_scalingpolicy_id FOREIGN KEY (scalingpolicy_id) REFERENCES scalingpolicy(id)
);

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS cb_stack_name VARCHAR(255);

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS cb_stack_type VARCHAR(255);

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS cloud_platform VARCHAR(255);

ALTER TABLE clusterpertain ADD COLUMN IF NOT EXISTS usercrn VARCHAR(512);

CREATE INDEX IF NOT EXISTS idx_cluster_cb_stack_type ON cluster (cb_stack_type);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_cluster_cb_stack_type;

ALTER TABLE clusterpertain DROP COLUMN IF EXISTS usercrn;

ALTER TABLE cluster DROP COLUMN IF EXISTS cb_stack_name;

ALTER TABLE cluster DROP COLUMN IF EXISTS cb_stack_type;

ALTER TABLE cluster DROP COLUMN IF EXISTS cloud_platform;

DROP TABLE IF EXISTS loadalert;
