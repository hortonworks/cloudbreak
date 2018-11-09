package com.sequenceiq.cloudbreak.template.views;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.template.views.dialect.DefaultRdsViewDialect;
import com.sequenceiq.cloudbreak.template.views.dialect.OracleRdsViewDialect;
import com.sequenceiq.cloudbreak.template.views.dialect.RdsViewDialect;
import com.sequenceiq.cloudbreak.template.views.dialect.ServiceIdOracleRdsViewDialect;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

public class RdsView {

    public static final String WITHOUT_JDBC_PREFIX_REGEX = "^(.*?):(\\d*)[:/]?(\\w+)?";

    private static final int MAX_GROUP_COUNT_NUMBER = 3;

    private final String connectionURL;

    private final String connectionDriver;

    private final String connectionUserName;

    private final String connectionPassword;

    private final String databaseName;

    private final String host;

    private final String hostWithPortWithJdbc;

    private final String subprotocol;

    private final String connectionString;

    private final String port;

    private final DatabaseVendor databaseVendor;

    private final String withoutJDBCPrefix;

    private final RdsViewDialect rdsViewDialect;

    public RdsView(RDSConfig rdsConfig) {
        connectionURL = rdsConfig.getConnectionURL();
        connectionUserName = rdsConfig.getConnectionUserName().getRaw();
        connectionPassword = rdsConfig.getConnectionPassword().getRaw();
        rdsViewDialect = createDialect(rdsConfig);
        String[] split = connectionURL.split(rdsViewDialect.jdbcPrefixSplitter());
        subprotocol = getSubprotocol(split);

        withoutJDBCPrefix = split[split.length - 1];

        Pattern compile = Pattern.compile(WITHOUT_JDBC_PREFIX_REGEX);
        Matcher matcher = compile.matcher(withoutJDBCPrefix);

        int hostGroupIndex = 1;
        int hostPortIndex = 2;
        int databaseGroupIndex = MAX_GROUP_COUNT_NUMBER;

        matcher.find();
        host = matcher.group(hostGroupIndex);
        port = matcher.group(hostPortIndex);
        databaseName = matcher.group(databaseGroupIndex);

        hostWithPortWithJdbc = connectionURL.replaceAll(rdsViewDialect.databaseNameSplitter() + databaseName + '$', "");
        connectionDriver = rdsConfig.getConnectionDriver();
        databaseVendor = rdsConfig.getDatabaseEngine();
        String pattern;
        if (rdsConfig.getType().equalsIgnoreCase(RdsType.RANGER.name())) {
            pattern = databaseVendor == DatabaseVendor.ORACLE11 || databaseVendor == DatabaseVendor.ORACLE12
                    ? "%s:%s" + rdsViewDialect.databaseNameSplitter() + "%s"
                    : "%s:%s";
            connectionString = String.format(pattern, host, port, databaseName);
        } else {
            connectionString = connectionURL;
        }
    }

    private RdsViewDialect createDialect(RDSConfig rdsConfig) {
        RdsViewDialect dialect;
        if (rdsConfig.getDatabaseEngine() == DatabaseVendor.ORACLE11 || rdsConfig.getDatabaseEngine() == DatabaseVendor.ORACLE12) {
            dialect = rdsConfig.getConnectionURL().lastIndexOf('/') > 0 ? new ServiceIdOracleRdsViewDialect() : new OracleRdsViewDialect();
        } else {
            dialect = new DefaultRdsViewDialect();
        }
        return dialect;
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
        return databaseVendor.databaseType();
    }

    public String getHost() {
        return host;
    }

    public String getConnectionDriver() {
        return connectionDriver;
    }

    public String getLowerCaseDatabaseEngine() {
        return databaseVendor.databaseType().toLowerCase();
    }

    public String getDatabaseEngine() {
        return databaseVendor.databaseType().toLowerCase();
    }

    public String getSubprotocol() {
        return subprotocol;
    }

    public String getPort() {
        return port;
    }

    public String getFancyName() {
        return databaseVendor.fancyName();
    }

    public String getDisplayName() {
        return databaseVendor.displayName();
    }

    public String getAmbariVendor() {
        return databaseVendor.databaseType();
    }

    // For ambari init backward compatibility
    public String getVendor() {
        return getAmbariVendor();
    }

    // For ambari init backward compatibility
    public String getUserName() {
        return connectionUserName;
    }

    // For ambari init backward compatibility
    public String getPassword() {
        return connectionPassword;
    }

    // For ambari init backward compatibility
    public String getName() {
        return databaseName;
    }

    public String getUpperCaseDatabaseEngine() {
        return databaseVendor.databaseType().toUpperCase();
    }

    public String getHostWithPortWithJdbc() {
        return hostWithPortWithJdbc;
    }

    public String getWithoutJDBCPrefix() {
        return withoutJDBCPrefix;
    }

    public String getConnectionString() {
        return connectionString;
    }

    private String getDatabaseName(String connectionURL) {
        String dbName = "";
        String[] split = connectionURL.split(rdsViewDialect.databaseNameSplitter());
        if (split.length > 1) {
            dbName = split[split.length - 1];
        }
        return dbName;
    }

    private String getSubprotocol(String[] split) {
        String databaseType = "";
        if (split.length > 1) {
            String firstPart = split[0];
            int firstIndexOfColon = firstPart.indexOf(':');
            int lastIndexOfColon = firstPart.lastIndexOf(':');
            databaseType = firstIndexOfColon < lastIndexOfColon
                    ? firstPart.substring(firstIndexOfColon + 1, lastIndexOfColon)
                    : firstPart.substring(firstIndexOfColon + 1);
        }
        return databaseType;
    }
}
