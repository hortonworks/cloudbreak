# Usage collection

1. CDP components use a common schema to log usage events.
    1. The schema is stored in usage.proto in this project.
    2. The source of truth for usage.proto is at https://github.infra.cloudera.com/thunderhead/thunderhead/blob/master/services/libs/protocols/src/main/proto/usage.proto
    3. To edit the event schema, first you need to edit the source of truth, and then copy-paste the file in this repository.
2. These usage events end up in ELK service.
3. The EDH team periodically consumes these logs and builds a Data Warehouse.