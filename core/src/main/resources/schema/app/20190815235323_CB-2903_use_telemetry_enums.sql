-- // CB-2903 Use enums instead of booleans
-- Migration SQL that makes the change goes here.

UPDATE
   component SET attributes = jsonb_set(attributes::jsonb,
                 '{reportDeploymentLogs}' , '"DISABLED"' )::text
WHERE attributes is not null AND componenttype = 'TELEMETRY';

UPDATE
   component SET attributes = (attributes::jsonb - 'meteringEnabled' )::text
WHERE attributes is not null AND componenttype = 'TELEMETRY';

UPDATE
   component SET attributes = jsonb_set(attributes::jsonb,
                 '{metering}' , '"DISABLED"' )::text
WHERE attributes is not null AND componenttype = 'TELEMETRY';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE
   component SET attributes = jsonb_set(attributes::jsonb,
                 '{reportDeploymentLogs}' , 'false' )::text
WHERE attributes is not null AND componenttype = 'TELEMETRY';

UPDATE
   component SET attributes = (attributes::jsonb - 'metering' )::text
WHERE attributes is not null AND componenttype = 'TELEMETRY';

UPDATE
   component SET attributes = jsonb_set(attributes::jsonb,
                 '{meteringEnabled}' , 'false' )::text
WHERE attributes is not null AND componenttype = 'TELEMETRY';
