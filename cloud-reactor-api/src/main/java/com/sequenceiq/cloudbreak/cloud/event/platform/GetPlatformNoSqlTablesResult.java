package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;

public class GetPlatformNoSqlTablesResult extends CloudPlatformResult {
    private CloudNoSqlTables noSqlTables;

    public GetPlatformNoSqlTablesResult(Long resourceId, CloudNoSqlTables noSqlTables) {
        super(resourceId);
        this.noSqlTables = noSqlTables;
    }

    public GetPlatformNoSqlTablesResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public CloudNoSqlTables getNoSqlTables() {
        return noSqlTables;
    }
}
