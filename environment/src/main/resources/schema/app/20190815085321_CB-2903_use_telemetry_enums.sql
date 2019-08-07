-- // CB-2903 Use enums instead of booleans
-- Migration SQL that makes the change goes here.

UPDATE
   environment SET telemetry = jsonb_set(telemetry::jsonb,
                 '{reportDeploymentLogs}' , '"DISABLED"' )::text
WHERE telemetry is not null;

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE
   environment SET telemetry = jsonb_set(telemetry::jsonb,
                 '{reportDeploymentLogs}' , 'false' )::text
WHERE telemetry is not null;
