package com.sequenceiq.cloudbreak.blueprint.template.views;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.blueprint.template.views.dialect.DefaultRdsViewDialect;
import com.sequenceiq.cloudbreak.blueprint.template.views.dialect.OracleRdsViewDialect;
import com.sequenceiq.cloudbreak.blueprint.template.views.dialect.RdsViewDialect;
import com.sequenceiq.cloudbreak.blueprint.template.views.dialect.ServiceIdOracleRdsViewDialect;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

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

    private String port;

    private String fancyName;

    private String databaseType;

    private String ambariVendor;

    private String upperCaseDatabaseEngine;

    private RdsViewDialect rdsViewDialect;

    private String withoutJDBCPrefix;

    public RdsView(RDSConfig rdsConfig) {
        connectionURL = rdsConfig.getConnectionURL();
        connectionUserName = rdsConfig.getConnectionUserName();
        connectionPassword = rdsConfig.getConnectionPassword();
        createDialect(rdsConfig);
        String[] split = connectionURL.split(rdsViewDialect.jdbcPrefixSplitter());
        subprotocol = getSubprotocol(split);

        withoutJDBCPrefix = split[split.length - 1];
        int portDelimiterIndex = withoutJDBCPrefix.indexOf(':');
        port = "";
        if (portDelimiterIndex > 0) {
            host = withoutJDBCPrefix.substring(0, portDelimiterIndex);
            int i = withoutJDBCPrefix.indexOf(rdsViewDialect.databaseNameSplitter(), portDelimiterIndex + 1);
            if (i > 0) {
                port = withoutJDBCPrefix.substring(portDelimiterIndex + 1, i);
            }
            if (i == -1 && host.length() < withoutJDBCPrefix.length()) {
                port = withoutJDBCPrefix.substring(portDelimiterIndex + 1, withoutJDBCPrefix.length());
            }
        } else {
            int i = withoutJDBCPrefix.indexOf(rdsViewDialect.databaseNameSplitter());
            host = i > 0 ? withoutJDBCPrefix.substring(0, i) : withoutJDBCPrefix;
        }

        databaseName = getDatabaseName(connectionURL);
        hostWithPortWithJdbc = connectionURL.replaceAll(rdsViewDialect.databaseNameSplitter() + databaseName + "$", "");
        hostWithPort = createConnectionHost();
        connectionDriver = rdsConfig.getConnectionDriver();
        if (rdsConfig.getDatabaseEngine() != null) {
            DatabaseVendor databaseVendor = DatabaseVendor.valueOf(rdsConfig.getDatabaseEngine());
            lowerCaseDatabaseEngine = databaseVendor.ambariVendor().toLowerCase();
            fancyName = databaseVendor.fancyName();
            ambariVendor = databaseVendor.ambariVendor();
            databaseType = databaseVendor.databaseType();
            upperCaseDatabaseEngine = databaseVendor.ambariVendor().toUpperCase();
        }
    }

    private void createDialect(RDSConfig rdsConfig) {
        if (rdsConfig.getDatabaseEngine().equals(DatabaseVendor.ORACLE11.name()) || rdsConfig.getDatabaseEngine().equals(DatabaseVendor.ORACLE12.name())) {
            rdsViewDialect = rdsConfig.getConnectionURL().lastIndexOf("/") > 0 ? new ServiceIdOracleRdsViewDialect() : new OracleRdsViewDialect();
        } else {
            rdsViewDialect = new DefaultRdsViewDialect();
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

    public String getDatabaseType() {
        return databaseType;
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

    public String getWithoutJDBCPrefix() {
        return withoutJDBCPrefix;
    }

    public void setWithoutJDBCPrefix(String withoutJDBCPrefix) {
        this.withoutJDBCPrefix = withoutJDBCPrefix;
    }

    private String getDatabaseName(String connectionURL) {
        String databaseName = "";
        String[] split = connectionURL.split(rdsViewDialect.databaseNameSplitter());
        if (split.length > 1) {
            databaseName = split[split.length - 1];
        }
        return databaseName;
    }

    private String createConnectionHost() {
        String result = host;
        if (StringUtils.isNotBlank(port)) {
            result = host + ':' + port;
        }
        return result;
    }

    private String getSubprotocol(String[] split) {
        String databaseType = "";
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
