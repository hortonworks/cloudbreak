package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.template.views;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.domain.RDSConfig;

public class RdsView {

    private String connectionURL;

    private String connectionUserName;

    private String connectionPassword;

    private String databaseName;

    private String host;

    private String connectionHost;

    private Map<String, Object> properties = new HashMap<>();

    public RdsView(RDSConfig rdsConfig) {
        this.connectionURL = rdsConfig.getConnectionURL();
        this.connectionUserName = rdsConfig.getConnectionUserName();
        this.connectionPassword = rdsConfig.getConnectionPassword();
        String[] split = rdsConfig.getConnectionURL().split("/");
        this.databaseName = split[split.length - 1];
        split = rdsConfig.getConnectionURL().split("//");
        split = split[1].split(":");
        this.host = split[0];
        this.connectionHost =  this.host + ":" + split[1].replace("/" + this.databaseName, "");
        if (rdsConfig.getAttributes() != null) {
            properties = rdsConfig.getAttributes().getMap();
        }
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

    public String getDatabaseName() {
        return databaseName;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getConnectionHost() {
        return connectionHost;
    }

    public String getHost() {
        return host;
    }
}
