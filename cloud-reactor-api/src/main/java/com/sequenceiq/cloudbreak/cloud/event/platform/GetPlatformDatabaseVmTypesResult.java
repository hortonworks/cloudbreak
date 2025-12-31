package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudDatabaseVmTypes;

public class GetPlatformDatabaseVmTypesResult extends CloudPlatformResult {
    private CloudDatabaseVmTypes databaseVmTypes;

    public GetPlatformDatabaseVmTypesResult(Long resourceId, CloudDatabaseVmTypes databaseVmTypes) {
        super(resourceId);
        this.databaseVmTypes = databaseVmTypes;
    }

    public GetPlatformDatabaseVmTypesResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public CloudDatabaseVmTypes getCloudDatabaseVmTypes() {
        return databaseVmTypes;
    }

    @Override
    public String toString() {
        return "GetPlatformVmTypesResult{"
                + "status=" + getStatus()
                + ", statusReason='" + getStatusReason() + '\''
                + ", errorDetails='" + getErrorDetails() + '\''
                + ", resourceId='" + getResourceId() + '\''
                + ", databaseVmTypes='" + databaseVmTypes + '\''
                + '}';
    }
}
