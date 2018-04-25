package com.sequenceiq.cloudbreak.blueprint.template.views;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.blueprint.template.views.dialect.DefaultRdsViewDialect;
import com.sequenceiq.cloudbreak.blueprint.template.views.dialect.OracleRdsViewDialect;
import com.sequenceiq.cloudbreak.blueprint.template.views.dialect.RdsViewDialect;
import com.sequenceiq.cloudbreak.blueprint.template.views.dialect.ServiceIdOracleRdsViewDialect;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

public class RdsView {

    private static final String WITHOUT_JDBC_PREFIX_REGEX = "^(.*?):(\\d*)[:/]?(\\w+)?";

    private static final int MAX_GROUP_COUNT_NUMBER = 3;

    private final String connectionURL;

    private final String connectionDriver;

    private final String connectionUserName;

    // For ambari init backward compatibility
    private final String userName;

    private final String connectionPassword;

    // For ambari init backward compatibility
    private final String password;

    private final String databaseName;

    // For ambari init backward compatibility
    private final String name;

    private final String host;

    private final String hostWithPortWithJdbc;

    private final String subprotocol;

    private final String lowerCaseDatabaseEngine;

    private final String connectionString;

    private final String port;

    private final String fancyName;

    private final String databaseType;

    private final String ambariVendor;

    private final String upperCaseDatabaseEngine;

    // For ambari init backward compatibility
    private String vendor;

    private final String withoutJDBCPrefix;

    private final RdsViewDialect rdsViewDialect;

    public RdsView(RDSConfig rdsConfig) {
        connectionURL = rdsConfig.getConnectionURL();
        connectionUserName = rdsConfig.getConnectionUserName();
        userName = connectionUserName;
        connectionPassword = rdsConfig.getConnectionPassword();
        password = connectionPassword;
        rdsViewDialect = createDialect(rdsConfig);
        String[] split = connectionURL.split(rdsViewDialect.jdbcPrefixSplitter());
        subprotocol = getSubprotocol(split);

        withoutJDBCPrefix = split[split.length - 1];

        Pattern compile = Pattern.compile(WITHOUT_JDBC_PREFIX_REGEX);
        Matcher matcher = compile.matcher(withoutJDBCPrefix);

        int hostGroupIndex = 1;
        int hostPortIndex = 2;
        int databaseGroupIndex = MAX_GROUP_COUNT_NUMBER;
        if (!matcher.find() || matcher.groupCount() != MAX_GROUP_COUNT_NUMBER
                || StringUtils.isEmpty(matcher.group(hostGroupIndex))
                || StringUtils.isEmpty(matcher.group(hostPortIndex))
                || StringUtils.isEmpty(matcher.group(databaseGroupIndex))) {
            throw new BadRequestException("Invalid JDBC URL");
        }

        host = matcher.group(hostGroupIndex);
        port = matcher.group(hostPortIndex);
        databaseName = matcher.group(databaseGroupIndex);
        name = databaseName;

        hostWithPortWithJdbc = connectionURL.replaceAll(rdsViewDialect.databaseNameSplitter() + databaseName + "$", "");
        connectionDriver = rdsConfig.getConnectionDriver();
        DatabaseVendor databaseVendor = DatabaseVendor.valueOf(rdsConfig.getDatabaseEngine());
        lowerCaseDatabaseEngine = databaseVendor.ambariVendor().toLowerCase();
        fancyName = databaseVendor.fancyName();
        ambariVendor = databaseVendor.ambariVendor();
        vendor = ambariVendor;
        databaseType = databaseVendor.databaseType();
        upperCaseDatabaseEngine = databaseVendor.ambariVendor().toUpperCase();
        String pattern;
        if (rdsConfig.getType().equalsIgnoreCase(RdsType.RANGER.name())) {
            if (databaseVendor == DatabaseVendor.ORACLE11 || databaseVendor == DatabaseVendor.ORACLE12) {
                pattern = "%s:%s" + rdsViewDialect.databaseNameSplitter() + "%s";
            } else {
                pattern = "%s:%s";
            }
            connectionString = String.format(pattern, host, port, databaseName);
        } else {
            connectionString = connectionURL;
        }
    }

    private RdsViewDialect createDialect(RDSConfig rdsConfig) {
        RdsViewDialect rdsViewDialect;
        if (rdsConfig.getDatabaseEngine().equals(DatabaseVendor.ORACLE11.name()) || rdsConfig.getDatabaseEngine().equals(DatabaseVendor.ORACLE12.name())) {
            rdsViewDialect = rdsConfig.getConnectionURL().lastIndexOf("/") > 0 ? new ServiceIdOracleRdsViewDialect() : new OracleRdsViewDialect();
        } else {
            rdsViewDialect = new DefaultRdsViewDialect();
        }
        return rdsViewDialect;
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

    public String getVendor() {
        return vendor;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
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

    public String getConnectionString() {
        return connectionString;
    }

    private String getDatabaseName(String connectionURL) {
        String databaseName = "";
        String[] split = connectionURL.split(rdsViewDialect.databaseNameSplitter());
        if (split.length > 1) {
            databaseName = split[split.length - 1];
        }
        return databaseName;
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
