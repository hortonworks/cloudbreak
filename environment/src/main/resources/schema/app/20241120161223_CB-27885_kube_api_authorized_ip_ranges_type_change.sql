-- // CB-27885 -- kubeApiAuthorizedIpRanges - ERROR: value too long for type character varying(255)
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ALTER COLUMN compute_kube_api_authorized_ip_ranges type text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment ALTER COLUMN compute_kube_api_authorized_ip_ranges type varchar(255);

