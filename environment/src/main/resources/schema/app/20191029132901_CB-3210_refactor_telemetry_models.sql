-- // CB-3210 refactor telemetry models (v2)
-- Migration SQL that makes the change goes here.

UPDATE
   environment SET telemetry = jsonb_set(telemetry::jsonb,
                 '{features}' , '{}' )::text
WHERE telemetry is not null AND telemetry::text <> 'null' AND telemetry::jsonb->'features' is not null;

UPDATE
   environment SET telemetry = jsonb_set(telemetry::jsonb,
                 '{fluentAttributes}' , '{}' )::text
WHERE telemetry is not null AND telemetry::text <> 'null' AND telemetry::jsonb->'fluentAttributes' is not null;

UPDATE
   environment SET telemetry = jsonb_set(telemetry::jsonb,
                 '{features,workloadAnalytics}' , '{"enabled": true}' )::text
WHERE telemetry is not null AND telemetry::text <> 'null' AND telemetry::jsonb->'features' is not null;

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE
    environment SET telemetry = (telemetry::jsonb #- '{features,workloadAnalytics}' )::text
WHERE telemetry is not null AND telemetry::text <> 'null' AND telemetry::jsonb->'features' is not null;