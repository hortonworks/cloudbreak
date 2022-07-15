package com.sequenceiq.cloudbreak.cloud.aws.view;

import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsVersionOperations;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;

public class AwsRdsDbParameterGroupView {

    @VisibleForTesting
    static final String ENGINE_VERSION = "engineVersion";

    private static final Pattern ENGINE_VERSION_PATTERN = Pattern.compile("^(\\d+)(?:\\.\\d+)?$");

    private static final int GROUP_MAJOR_VERSION = 1;

    private static final int VERSION_9 = 9;

    private static final int VERSION_13 = 13;

    private static final String POSTGRES = "postgres";

    private final DatabaseServer databaseServer;

    private final AwsRdsVersionOperations awsRdsVersionOperations;

    public AwsRdsDbParameterGroupView(DatabaseServer databaseServer, AwsRdsVersionOperations awsRdsVersionOperations) {
        this.databaseServer = databaseServer;
        this.awsRdsVersionOperations = awsRdsVersionOperations;
    }

    public String getDBParameterGroupName() {
        return databaseServer.getServerId() != null ? "dpg-" + databaseServer.getServerId() : null;
    }

    public String getDBParameterGroupFamily() {
        DatabaseEngine engine = databaseServer.getEngine();
        if (engine == null) {
            return null;
        }
        String engineVersion = databaseServer.getStringParameter(ENGINE_VERSION);
        return awsRdsVersionOperations.getDBParameterGroupFamily(engine, engineVersion);
    }

}
