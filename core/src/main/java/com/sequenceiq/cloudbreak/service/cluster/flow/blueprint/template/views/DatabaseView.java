package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.template.views;

import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;

public class DatabaseView {

    private final String connectionURL;

    private final String connectionUserName;

    private final String connectionPassword;

    public DatabaseView(AmbariDatabase ambariDatabase) {
        connectionURL = ambariDatabase.getHost() + ':' + ambariDatabase.getPort() + '/' + ambariDatabase.getName();
        connectionUserName = ambariDatabase.getUserName();
        connectionPassword = ambariDatabase.getPassword();
    }

    public String getConnectionURL() {
        return connectionURL;
    }

    public String getConnectionUserName() {
        return connectionUserName;
    }

    public String getConnectionPassword() {
        return connectionPassword;
    }
}
