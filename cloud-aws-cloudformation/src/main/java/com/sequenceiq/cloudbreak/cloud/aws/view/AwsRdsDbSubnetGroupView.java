package com.sequenceiq.cloudbreak.cloud.aws.view;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;

public class AwsRdsDbSubnetGroupView {

    private final DatabaseServer databaseServer;

    public AwsRdsDbSubnetGroupView(DatabaseServer databaseServer) {
        this.databaseServer = databaseServer;
    }

    public String getDBSubnetGroupName() {
        return databaseServer.getServerId() != null ? "dsg-" + databaseServer.getServerId() : null;
    }
}
