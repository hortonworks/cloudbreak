package com.sequenceiq.cloudbreak.cloud.aws.view;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;

public class AwsRdsVpcSecurityGroupView {
    private final DatabaseServer databaseServer;

    public AwsRdsVpcSecurityGroupView(DatabaseServer databaseServer) {
        this.databaseServer = databaseServer;
    }

    public String getDBSecurityGroupName() {
        return databaseServer.getServerId() != null ? "dsecg-" + databaseServer.getServerId() : null;
    }

}
