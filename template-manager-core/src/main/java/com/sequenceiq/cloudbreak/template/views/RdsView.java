package com.sequenceiq.cloudbreak.template.views;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.template.views.dialect.DefaultRdsViewDialect;
import com.sequenceiq.cloudbreak.template.views.dialect.OracleRdsViewDialect;
import com.sequenceiq.cloudbreak.template.views.dialect.RdsViewDialect;
import com.sequenceiq.cloudbreak.template.views.dialect.ServiceIdOracleRdsViewDialect;

public class RdsView {

    public static final String WITHOUT_JDBC_PREFIX_REGEX = "^(.*?):(\\d*)[:/]?(\\w+)?";

    private static final Pattern WITHOUT_JDBC_PREFIX_PATTERN = Pattern.compile(WITHOUT_JDBC_PREFIX_REGEX);

    private static final int HOST_GROUP_INDEX = 1;

    private static final int PORT_GROUP_INDEX = 2;

    private static final int DATABASE_GROUP_INDEX = 3;

    private static final String SSL_OPTIONS_WITHOUT_CERTIFICATE_FILE_PATH = "sslmode=verify-full&sslrootcert=";

    private String connectionURL;

    private boolean useSsl;

    private String sslCertificateFilePath;

    private String connectionDriver;

    private String connectionUserName;

    private String connectionPassword;

    private String databaseName;

    private String host;

    private String hostWithPortWithJdbc;

    private String subprotocol;

    private String connectionString;

    private String port;

    private DatabaseVendor databaseVendor;

    private String withoutJDBCPrefix;

    private RdsViewDialect rdsViewDialect;

    public RdsView(RDSConfig rdsConfig) {
        this(rdsConfig, "");
    }

    public RdsView(RDSConfig rdsConfig, String sslCertificateFilePath) {
        this.sslCertificateFilePath = Objects.requireNonNullElse(sslCertificateFilePath, "");
        setup(rdsConfig);
    }

    public RdsView(RdsConfigWithoutCluster rdsConfig) {
        this(rdsConfig, "");
    }

    public RdsView(RdsConfigWithoutCluster rdsConfigWithoutCluster, String sslCertificateFilePath) {
        this.sslCertificateFilePath = Objects.requireNonNullElse(sslCertificateFilePath, "");
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setArchived(rdsConfigWithoutCluster.isArchived());
        rdsConfig.setConnectionDriver(rdsConfigWithoutCluster.getConnectionDriver());
        rdsConfig.setConnectionURL(rdsConfigWithoutCluster.getConnectionURL());
        rdsConfig.setConnectionPassword(rdsConfigWithoutCluster.getConnectionPassword().getRaw());
        rdsConfig.setConnectionUserName(rdsConfigWithoutCluster.getConnectionUserName().getRaw());
        rdsConfig.setConnectorJarUrl(rdsConfigWithoutCluster.getConnectorJarUrl());
        rdsConfig.setCreationDate(rdsConfigWithoutCluster.getCreationDate());
        rdsConfig.setDatabaseEngine(rdsConfigWithoutCluster.getDatabaseEngine());
        rdsConfig.setDeletionTimestamp(rdsConfigWithoutCluster.getDeletionTimestamp());
        rdsConfig.setDescription(rdsConfigWithoutCluster.getDescription());
        rdsConfig.setId(rdsConfigWithoutCluster.getId());
        rdsConfig.setName(rdsConfigWithoutCluster.getName());
        rdsConfig.setSslMode(rdsConfigWithoutCluster.getSslMode());
        rdsConfig.setType(rdsConfigWithoutCluster.getType());
        setup(rdsConfig);
    }

    private void setup(RDSConfig rdsConfig) {
        // Note: any value is valid for sslCertificateFile for sake of backward compatibility.
        useSsl = RdsSslMode.isEnabled(rdsConfig.getSslMode());
        if (useSsl) {
            String configConnectionURL = rdsConfig.getConnectionURL();
            StringBuilder sb = new StringBuilder(configConnectionURL);
            if (configConnectionURL.contains("?")) {
                char lastChar = configConnectionURL.charAt(configConnectionURL.length() - 1);
                sb.append(lastChar == '?' || lastChar == '&' ? "" : "&");
            } else {
                sb.append('?');
            }
            sb.append(SSL_OPTIONS_WITHOUT_CERTIFICATE_FILE_PATH);
            sb.append(this.sslCertificateFilePath);
            connectionURL = sb.toString();
        } else {
            connectionURL = rdsConfig.getConnectionURL();
        }

        connectionUserName = rdsConfig.getConnectionUserName();
        connectionPassword = rdsConfig.getConnectionPassword();
        rdsViewDialect = createDialect(rdsConfig);
        String[] split = connectionURL.split(rdsViewDialect.jdbcPrefixSplitter());
        subprotocol = getSubprotocol(split);

        withoutJDBCPrefix = split[split.length - 1];

        Matcher matcher = WITHOUT_JDBC_PREFIX_PATTERN.matcher(withoutJDBCPrefix);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Malformed withoutJDBCPrefix: " + withoutJDBCPrefix);
        }
        host = matcher.group(HOST_GROUP_INDEX);
        port = matcher.group(PORT_GROUP_INDEX);
        databaseName = matcher.group(DATABASE_GROUP_INDEX);

        hostWithPortWithJdbc = connectionURL.replaceAll(rdsViewDialect.databaseNameSplitter() + databaseName + "(?:[?].*)?$", "");
        connectionDriver = rdsConfig.getConnectionDriver();
        databaseVendor = rdsConfig.getDatabaseEngine();

        if (rdsConfig.getType().equalsIgnoreCase(DatabaseType.RANGER.name())) {
            String pattern = databaseVendor == DatabaseVendor.ORACLE11 || databaseVendor == DatabaseVendor.ORACLE12
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

    public boolean isUseSsl() {
        return useSsl;
    }

    public String getSslCertificateFilePath() {
        return sslCertificateFilePath;
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

    public DatabaseVendor getDatabaseVendor() {
        return databaseVendor;
    }

    public String getFancyName() {
        return databaseVendor.fancyName();
    }

    public String getDisplayName() {
        return databaseVendor.displayName();
    }

    public String getClusterManagerVendor() {
        return databaseVendor.databaseType();
    }

    // For ambari init backward compatibility
    public String getVendor() {
        return getClusterManagerVendor();
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
