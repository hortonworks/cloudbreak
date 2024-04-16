-- // CB-25374 -- Add additional params to cdp cli for env v2 creation
-- Migration SQL that makes the change goes here.

ALTER TABLE environment RENAME COLUMN create_compute_cluster TO compute_create;

ALTER TABLE environment ADD COLUMN IF NOT EXISTS compute_private_cluster boolean NOT NULL DEFAULT false;

ALTER TABLE environment ADD COLUMN IF NOT EXISTS compute_kube_api_authorized_ip_ranges varchar(255);

ALTER TABLE environment ADD COLUMN IF NOT EXISTS compute_outbound_type varchar(255);

ALTER TABLE environment ADD COLUMN IF NOT EXISTS compute_load_balancer_authorized_ip_ranges varchar(255);


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment RENAME COLUMN compute_create TO create_compute_cluster;

ALTER TABLE environment DROP COLUMN IF EXISTS compute_private_cluster;

ALTER TABLE environment DROP COLUMN IF EXISTS compute_kube_api_authorized_ip_ranges;

ALTER TABLE environment DROP COLUMN IF EXISTS compute_outbound_type;

ALTER TABLE environment DROP COLUMN IF EXISTS compute_load_balancer_authorized_ip_ranges;

