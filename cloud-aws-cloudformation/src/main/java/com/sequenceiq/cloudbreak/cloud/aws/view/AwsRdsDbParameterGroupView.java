package com.sequenceiq.cloudbreak.cloud.aws.view;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;
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

    public AwsRdsDbParameterGroupView(DatabaseServer databaseServer) {
        this.databaseServer = databaseServer;
    }

    public String getDBParameterGroupName() {
        return databaseServer.getServerId() != null ? "dpg-" + databaseServer.getServerId() : null;
    }

    public String getDBParameterGroupFamily() {
        DatabaseEngine engine = databaseServer.getEngine();
        if (engine == null) {
            return null;
        }
        switch (engine) {
            case POSTGRESQL:
                String engineVersion = databaseServer.getStringParameter(ENGINE_VERSION);
                String familyVersion = null;
                if (engineVersion != null) {
                    Matcher engineVersionMatcher = ENGINE_VERSION_PATTERN.matcher(engineVersion);
                    if (engineVersionMatcher.matches()) {
                        String engineMajorVersion = engineVersionMatcher.group(GROUP_MAJOR_VERSION);
                        int engineMajorVersionNumber = Integer.parseInt(engineMajorVersion);
                        if (engineMajorVersionNumber >= VERSION_9 && engineMajorVersionNumber <= VERSION_13) {
                            // Family version matches the engine version for 9.5 and 9.6, and simply equals the major version otherwise
                            familyVersion = engineMajorVersionNumber == VERSION_9 ? engineVersion : engineMajorVersion;
                        } else {
                            throw new IllegalStateException("Unsupported RDS POSTGRESQL engine version " + engineVersion);
                        }
                    } else {
                        throw new IllegalStateException("Unsupported RDS POSTGRESQL engine version " + engineVersion);
                    }
                }
                return "postgres" + familyVersion;
            default:
                throw new IllegalStateException("Unsupported RDS engine " + engine);
        }
    }

}
