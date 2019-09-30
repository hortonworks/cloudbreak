-- // CB-3210 refactor telemetry models
-- Migration SQL that makes the change goes here.
UPDATE
    environment SET telemetry = (telemetry::jsonb - 'reportDeploymentLogs' )::text
WHERE telemetry is not null;

UPDATE
    environment SET telemetry = (telemetry::jsonb - 'workloadAnalytics' )::text
WHERE telemetry is not null;

UPDATE
    environment SET telemetry = (telemetry::jsonb #- '{logging,attributes}' )::text
WHERE telemetry is not null AND telemetry::jsonb->'logging' is not null;

UPDATE
   environment SET telemetry = jsonb_set(telemetry::jsonb,
                 '{features}' , '{}' )::text
WHERE telemetry is not null;

UPDATE
   environment SET telemetry = jsonb_set(telemetry::jsonb,
                 '{fluentAttributes}' , '{}' )::text
WHERE telemetry is not null;

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE
    environment SET telemetry = (telemetry::jsonb - 'fluentAttributes' )::text
WHERE telemetry is not null;

UPDATE
    environment SET telemetry = (telemetry::jsonb - 'features' )::text
WHERE telemetry is not null;

UPDATE
   environment SET telemetry = jsonb_set(telemetry::jsonb,
                 '{logging,attributes}' , '{}' )::text
WHERE telemetry is not null AND telemetry::jsonb->'logging' is not null;
