-- // CB-3210 refactor telemetry models
-- Migration SQL that makes the change goes here.

UPDATE
    component SET attributes = (attributes::jsonb - 'reportDeploymentLogs' )::text
WHERE componenttype = 'TELEMETRY' AND attributes is not null;

UPDATE
   component SET attributes = jsonb_set(attributes::jsonb,
                 '{fluentAttributes}' , '{}' )::text
WHERE componenttype = 'TELEMETRY' AND attributes is not null;

UPDATE
   component SET attributes = jsonb_set(attributes::jsonb,
                 '{features}' , '{}' )::text
WHERE componenttype = 'TELEMETRY' AND attributes is not null;

UPDATE
   component SET attributes = jsonb_set(attributes::jsonb,
                 '{features,metering}' , '{"enabled": true}' )::text
WHERE componenttype = 'TELEMETRY' AND attributes::jsonb is not null AND attributes::jsonb->'meteringEnabled' is not null AND attributes::jsonb->'meteringEnabled' = 'true';

UPDATE
   component SET attributes = (attributes::jsonb - 'meteringEnabled' )::text
WHERE componenttype = 'TELEMETRY' AND attributes::jsonb is not null AND attributes::jsonb->'meteringEnabled' is not null;

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE
   component SET attributes = jsonb_set(attributes::jsonb,
                 '{meteringEnabled}' , 'true' )::text
WHERE componenttype = 'TELEMETRY' AND attributes::jsonb is not null AND attributes::jsonb->'features' is not null AND attributes::jsonb->'features'->'metering' is not null AND (attributes::jsonb->'features'->'metering')::text = '{"enabled": true}';

UPDATE
    component SET attributes = (attributes::jsonb - 'features' )::text
WHERE componenttype = 'TELEMETRY' AND attributes is not null;

UPDATE
    component SET attributes = (attributes::jsonb - 'fluentAttributes' )::text
WHERE componenttype = 'TELEMETRY' AND attributes is not null;