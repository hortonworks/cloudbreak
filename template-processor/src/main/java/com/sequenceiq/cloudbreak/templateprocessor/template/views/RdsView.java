package com.sequenceiq.cloudbreak.templateprocessor.template.views;

import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import org.apache.commons.lang3.StringUtils;

public class RdsView {

    private final String connectionURL;

    private final String connectionDriver;

    private final String connectionUserName;

    private final String connectionPassword;

    private final String databaseName;

    private final String host;

    private final String hostWithPort;

    private final String hostWithPortWithJdbc;

    private final String subprotocol;

    private String lowerCaseDatabaseEngine;

    private final String port;

    private String fancyName;

    private String ambariVendor;

    private String upperCaseDatabaseEngine;

    public RdsView(RDSConfig rdsConfig) {
        connectionURL = rdsConfig.getConnectionURL();
        connectionUserName = rdsConfig.getConnectionUserName();
        connectionPassword = rdsConfig.getConnectionPassword();

        String[] split = connectionURL.split("//");
        String withoutJDBCPrefix = split[split.length - 1];
        String hostWithPort = withoutJDBCPrefix.split("/")[0];
        int portDelimiterIndex = hostWithPort.indexOf(':');
        if (portDelimiterIndex > 0) {
            host = hostWithPort.substring(0, portDelimiterIndex);
            port = hostWithPort.substring(portDelimiterIndex + 1);
        } else {
            host = hostWithPort;
            port = "";
        }

        databaseName = getDatabaseName(connectionURL);
        hostWithPortWithJdbc = connectionURL.replace("/" + databaseName, "");
        this.hostWithPort = createConnectionHost(port);
        connectionDriver = rdsConfig.getConnectionDriver();
        subprotocol = getSubprotocol(connectionURL);
        if (rdsConfig.getDatabaseEngine() != null) {
            lowerCaseDatabaseEngine = rdsConfig.getDatabaseEngine().toLowerCase();
            DatabaseVendor databaseVendor = DatabaseVendor.valueOf(rdsConfig.getDatabaseEngine());
            fancyName = databaseVendor.fancyName();
            ambariVendor = databaseVendor.ambariVendor();
            upperCaseDatabaseEngine = rdsConfig.getDatabaseEngine().toUpperCase();
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

    public String getLowerCaseDatabaseEngine() {
        return lowerCaseDatabaseEngine;
    }

    public String getDatabaseEngine() {
        return lowerCaseDatabaseEngine;
    }

    public String getSubprotocol() {
        return subprotocol;
    }

    public String getPort() {
        return port;
    }

    public String getFancyName() {
        return fancyName;
    }

    public String getAmbariVendor() {
        return ambariVendor;
    }

    public String getUpperCaseDatabaseEngine() {
        return upperCaseDatabaseEngine;
    }

    public String getHostWithPortWithJdbc() {
        return hostWithPortWithJdbc;
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
        if (StringUtils.isNotBlank(port)) {
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
