-- // CB-29979 New operation endpoint for getting latest by env and type
-- Migration SQL that makes the change goes here.

create index if not exists operation_operationtype_idx
  on operation (operationtype);

-- //@UNDO
-- SQL to undo the change goes here.
