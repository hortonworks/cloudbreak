package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.template.views;

import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;

public class DatabaseView {

    private String connectionURL;

    private String connectionUserName;

    private String connectionPassword;

    public DatabaseView(AmbariDatabase ambariDatabase) {
        this.connectionURL = ambariDatabase.getHost() + ":" + ambariDatabase.getPort() + "/" + ambariDatabase.getName();
        this.connectionUserName = ambariDatabase.getUserName();
        this.connectionPassword = ambariDatabase.getPassword();
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
