package com.sequenceiq.cloudbreak.blueprint.template.views;

import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.domain.RDSConfig;

public class RdsView {

    private final String connectionURL;

    private final String connectionDriver;

    private final String connectionUserName;

    private final String connectionPassword;

    private final String databaseName;

    private final String host;

    private final String hostWithPort;

    private final String subprotocol;

    private String databaseEngine;

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
        this.hostWithPort = createConnectionHost(port);
        connectionDriver = rdsConfig.getConnectionDriver();
        subprotocol = getSubprotocol(connectionURL);
        if (rdsConfig.getDatabaseEngine() != null) {
            databaseEngine = rdsConfig.getDatabaseEngine().toLowerCase();
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

    public String getHostWithPort() {
        return hostWithPort;
    }

    public String getHost() {
        return host;
    }

    public String getConnectionDriver() {
        return connectionDriver;
    }

    public String getDatabaseEngine() {
        return databaseEngine;
    }

    public String getSubprotocol() {
        return subprotocol;
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

    private String getSubprotocol(String connectionURL) {
        String databaseType = "";
        String[] split = connectionURL.split("//");
        if (split.length > 1) {
            String firstPart = split[0];
            int firstIndexOfColon = firstPart.indexOf(":");
            int lastIndexOfColon = firstPart.lastIndexOf(":");
            if (firstIndexOfColon < lastIndexOfColon) {
                databaseType = firstPart.substring(firstIndexOfColon + 1, lastIndexOfColon);
            } else {
                databaseType = firstPart.substring(firstIndexOfColon + 1);
            }
        }
        return databaseType;
    }
}
