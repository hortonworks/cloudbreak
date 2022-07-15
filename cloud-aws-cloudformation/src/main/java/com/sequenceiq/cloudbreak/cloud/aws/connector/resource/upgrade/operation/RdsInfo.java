package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import java.util.Map;

public class RdsInfo {

    private final Map<String, String> dbArnToInstanceStatuses;

    private final RdsEngineVersion dbEngineVersion;

    private final RdsState rdsState;

    public RdsInfo(RdsState rdsState, Map<String, String> dbArnToInstanceStatuses, RdsEngineVersion dbEngineVersion) {
        this.rdsState = rdsState;
        this.dbArnToInstanceStatuses = dbArnToInstanceStatuses;
        this.dbEngineVersion = dbEngineVersion;
    }

    public RdsState getRdsState() {
        return rdsState;
    }

    public Map<String, String> getDbArnToInstanceStatuses() {
        return dbArnToInstanceStatuses;
    }

    public RdsEngineVersion getRdsEngineVersion() {
        return dbEngineVersion;
    }

    @Override
    public String toString() {
        return "RdsInfo{" +
                "dbArnToInstanceStatuses=" + dbArnToInstanceStatuses +
                ", dbEngineVersions=" + dbEngineVersion +
                ", rdsState=" + rdsState +
                '}';
    }

}