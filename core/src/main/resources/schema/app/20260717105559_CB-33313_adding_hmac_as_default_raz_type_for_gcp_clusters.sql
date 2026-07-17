-- // CB-33313 adding hmac as default raz type for gcp clusters
-- Migration SQL that makes the change goes here.

INSERT INTO stack_parameters (stack_id, value, "key")
SELECT s.id, 'HMAC', 'razAuthenticationType'
FROM stack s JOIN cluster c ON c.stack_id = s.id
WHERE s.cloudplatform = 'GCP'
  AND s.terminated IS NULL
  AND c.ranger_raz_enabled = true
  AND s.type = 'DATALAKE'
  AND NOT EXISTS (
    SELECT 1 FROM stack_parameters sp
    WHERE sp.stack_id = s.id AND sp."key" = 'razAuthenticationType'
);

-- //@UNDO
-- No undo needed as the parameter for earlier RAZ enabled GCP clusters was HMAC by default
