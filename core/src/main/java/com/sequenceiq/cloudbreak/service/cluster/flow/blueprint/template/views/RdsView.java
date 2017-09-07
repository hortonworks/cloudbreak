package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.template.views;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.domain.RDSConfig;

public class RdsView {

    private final String connectionURL;

    private final String connectionUserName;

    private final String connectionPassword;

    private final String databaseName;

    private final String host;

    private final String connectionHost;

    private Map<String, Object> properties = new HashMap<>();

    public RdsView(RDSConfig rdsConfig) {
        connectionURL = rdsConfig.getConnectionURL();
        connectionUserName = rdsConfig.getConnectionUserName();
        connectionPassword = rdsConfig.getConnectionPassword();

        String port = "";
        String[] split = connectionURL.split("//");
        String withoutJDBCPrefix = split[split.length - 1];
        String hostWithPort = withoutJDBCPrefix.split("/")[0];
        int portDelimiterIndex = hostWithPort.indexOf(':');
        if (portDelimiterIndex > 0) {
            host = hostWithPort.substring(0, portDelimiterIndex);
            port = hostWithPort.substring(portDelimiterIndex + 1);
        } else {
            host = hostWithPort;
        }

        databaseName = getDatabaseName(connectionURL);
        connectionHost = createConnectionHost(port);
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

    private String getDatabaseName(String connectionURL) {
        String databaseName = "";
        String[] split = connectionURL.split("/");
        if (split.length > 1) {
            databaseName = split[split.length - 1];
        }
        return databaseName;
    }

    private String createConnectionHost(String port) {
        String result = host;
        if (!StringUtils.isEmpty(port)) {
            result = host + ':' + port;
        }
        return result;
    }
}
