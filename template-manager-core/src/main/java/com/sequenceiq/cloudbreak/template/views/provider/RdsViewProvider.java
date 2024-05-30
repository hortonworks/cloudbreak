package com.sequenceiq.cloudbreak.template.views.provider;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import com.sequenceiq.cloudbreak.template.views.dialect.DefaultRdsViewDialect;
import com.sequenceiq.cloudbreak.template.views.dialect.OracleRdsViewDialect;
import com.sequenceiq.cloudbreak.template.views.dialect.RdsViewDialect;
import com.sequenceiq.cloudbreak.template.views.dialect.ServiceIdOracleRdsViewDialect;

@Component
public class RdsViewProvider {

    public static final String WITHOUT_JDBC_PREFIX_REGEX = "^(.*?):(\\d*)[:/]?(\\w+)?";

    private static final Pattern WITHOUT_JDBC_PREFIX_PATTERN = Pattern.compile(WITHOUT_JDBC_PREFIX_REGEX);

    private static final String VERIFY_FULL_SSL_OPTIONS_WITHOUT_CERTIFICATE_FILE_PATH = "sslmode=verify-full&sslrootcert=";

    private static final String SSL_MODE = "sslmode=";

    private static final String ROOT_CERT_PATH = "&sslrootcert=";

    private static final int HOST_GROUP_INDEX = 1;

    private static final int PORT_GROUP_INDEX = 2;

    private static final int DATABASE_GROUP_INDEX = 3;

    @Value("${cb.externaldatabase.gcp.sslmode:verify-ca}")
    private String gcpExternalDatabaseSslVerificationMode;

    @Value("${cb.externaldatabase.ssl.rootcerts.path:/hadoopfs/fs1/database-cacerts/certs.pem}")
    private String rootCertsPath;

    public RdsView getRdsView(RdsConfigWithoutCluster rdsConfig, String cloudPlatform, boolean externalDatabaseRequested) {
        return getRdsView(rdsConfig, "", cloudPlatform, externalDatabaseRequested);
    }

    public RdsView getRdsView(RdsConfigWithoutCluster rdsConfigWithoutCluster, String sslCertificateFilePath, String cloudPlatform,
            boolean externalDatabaseRequested) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setArchived(rdsConfigWithoutCluster.isArchived());
        rdsConfig.setConnectionDriver(rdsConfigWithoutCluster.getConnectionDriver());
        rdsConfig.setConnectionURL(rdsConfigWithoutCluster.getConnectionURL());
        rdsConfig.setConnectionPassword(rdsConfigWithoutCluster.getConnectionPassword());
        rdsConfig.setConnectionUserName(rdsConfigWithoutCluster.getConnectionUserName());
        rdsConfig.setConnectorJarUrl(rdsConfigWithoutCluster.getConnectorJarUrl());
        rdsConfig.setCreationDate(rdsConfigWithoutCluster.getCreationDate());
        rdsConfig.setDatabaseEngine(rdsConfigWithoutCluster.getDatabaseEngine());
        rdsConfig.setDeletionTimestamp(rdsConfigWithoutCluster.getDeletionTimestamp());
        rdsConfig.setDescription(rdsConfigWithoutCluster.getDescription());
        rdsConfig.setId(rdsConfigWithoutCluster.getId());
        rdsConfig.setName(rdsConfigWithoutCluster.getName());
        rdsConfig.setSslMode(rdsConfigWithoutCluster.getSslMode());
        rdsConfig.setType(rdsConfigWithoutCluster.getType());
        return getRdsView(rdsConfig, Objects.requireNonNullElse(sslCertificateFilePath, rootCertsPath), cloudPlatform, externalDatabaseRequested);
    }

    public RdsView getRdsView(RDSConfig rdsConfig, String cloudPlatform, boolean externalDatabaseRequested) {
        return getRdsView(rdsConfig, "", cloudPlatform, externalDatabaseRequested);
    }

    public RdsView getRdsView(@Nonnull RDSConfig rdsConfig, String sslCertificateFilePath, String cloudPlatform, boolean externalDatabaseRequested) {
        RdsView rdsView = new RdsView();
        String sslPath = StringUtils.isNotEmpty(sslCertificateFilePath) ? sslCertificateFilePath : rootCertsPath;
        rdsView.setSslCertificateFilePath(sslPath);
        // Note: any value is valid for sslCertificateFile for sake of backward compatibility.
        rdsView.setUseSsl(RdsSslMode.isEnabled(rdsConfig.getSslMode()));
        if (rdsView.isUseSsl()) {
            String configConnectionURL = rdsConfig.getConnectionURL();
            StringBuilder sb = new StringBuilder(configConnectionURL);
            if (configConnectionURL.contains("?")) {
                char lastChar = configConnectionURL.charAt(configConnectionURL.length() - 1);
                sb.append(lastChar == '?' || lastChar == '&' ? "" : "&");
            } else {
                sb.append('?');
            }
            populateSSLMode(cloudPlatform, externalDatabaseRequested, sb);
            sb.append(rdsView.getSslCertificateFilePath());
            rdsView.setConnectionURL(sb.toString());
        } else {
            rdsView.setConnectionURL(rdsConfig.getConnectionURL());
        }

        rdsView.setConnectionUserName(rdsConfig.getConnectionUserName());
        rdsView.setConnectionPassword(rdsConfig.getConnectionPassword());
        RdsViewDialect dialect = createDialect(rdsConfig);
        rdsView.setRdsViewDialect(dialect);
        String[] split = rdsView.getConnectionURL().split(dialect.jdbcPrefixSplitter());
        rdsView.setSubprotocol(getSubprotocol(split));

        rdsView.setWithoutJDBCPrefix(split[split.length - 1]);
        rdsView.setType(rdsConfig.getType().toLowerCase(Locale.ROOT));

        Matcher matcher = WITHOUT_JDBC_PREFIX_PATTERN.matcher(rdsView.getWithoutJDBCPrefix());
        if (!matcher.find()) {
            throw new IllegalArgumentException("Malformed withoutJDBCPrefix: " + rdsView.getWithoutJDBCPrefix());
        }
        rdsView.setHost(matcher.group(HOST_GROUP_INDEX));
        rdsView.setPort(matcher.group(PORT_GROUP_INDEX));
        rdsView.setDatabaseName(matcher.group(DATABASE_GROUP_INDEX));

        rdsView.setHostWithPortWithJdbc(rdsView.getConnectionURL()
                .replaceAll(dialect.databaseNameSplitter() + rdsView.getDatabaseName() + "(?:[?].*)?$", ""));
        rdsView.setConnectionDriver(rdsConfig.getConnectionDriver());
        rdsView.setDatabaseVendor(rdsConfig.getDatabaseEngine());

        if (rdsConfig.getType().equalsIgnoreCase(DatabaseType.RANGER.name())) {
            String pattern = rdsView.getDatabaseVendor() == DatabaseVendor.ORACLE11 || rdsView.getDatabaseVendor() == DatabaseVendor.ORACLE12
                    ? "%s:%s" + dialect.databaseNameSplitter() + "%s"
                    : "%s:%s";
            rdsView.setConnectionString(String.format(pattern, rdsView.getHost(), rdsView.getPort(), rdsView.getDatabaseName()));
        } else {
            rdsView.setConnectionString(rdsView.getConnectionURL());
        }
        return rdsView;
    }

    private void populateSSLMode(String cloudPlatform, boolean externalDatabaseRequested, StringBuilder sb) {
        if (CloudPlatform.GCP.equalsIgnoreCase(cloudPlatform) && externalDatabaseRequested) {
            sb.append(SSL_MODE);
            sb.append(gcpExternalDatabaseSslVerificationMode);
            sb.append(ROOT_CERT_PATH);
        } else {
            sb.append(VERIFY_FULL_SSL_OPTIONS_WITHOUT_CERTIFICATE_FILE_PATH);
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
