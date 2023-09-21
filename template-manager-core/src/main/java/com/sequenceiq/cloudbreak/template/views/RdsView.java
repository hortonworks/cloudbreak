package com.sequenceiq.cloudbreak.template.views;

import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.template.views.dialect.RdsViewDialect;

public class RdsView {

    private static final String CONNECTION_URL_OPTIONS_DELIMITER = "?";

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

    @JsonIgnore
    private RdsViewDialect rdsViewDialect;

    private String type;

    public RdsView() {
    }

    public void setConnectionURL(String connectionURL) {
        this.connectionURL = connectionURL;
    }

    public void setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
    }

    public void setSslCertificateFilePath(String sslCertificateFilePath) {
        this.sslCertificateFilePath = sslCertificateFilePath;
    }

    public void setConnectionDriver(String connectionDriver) {
        this.connectionDriver = connectionDriver;
    }

    public void setConnectionUserName(String connectionUserName) {
        this.connectionUserName = connectionUserName;
    }

    public void setConnectionPassword(String connectionPassword) {
        this.connectionPassword = connectionPassword;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setHostWithPortWithJdbc(String hostWithPortWithJdbc) {
        this.hostWithPortWithJdbc = hostWithPortWithJdbc;
    }

    public void setSubprotocol(String subprotocol) {
        this.subprotocol = subprotocol;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setDatabaseVendor(DatabaseVendor databaseVendor) {
        this.databaseVendor = databaseVendor;
    }

    public void setWithoutJDBCPrefix(String withoutJDBCPrefix) {
        this.withoutJDBCPrefix = withoutJDBCPrefix;
    }

    public void setRdsViewDialect(RdsViewDialect rdsViewDialect) {
        this.rdsViewDialect = rdsViewDialect;
    }

    public String getConnectionURL() {
        return connectionURL;
    }

    @Nonnull
    public String getConnectionURLOptions() {
        return Objects.requireNonNullElse(connectionURL, "").contains(CONNECTION_URL_OPTIONS_DELIMITER) ?
                connectionURL.substring(connectionURL.indexOf(CONNECTION_URL_OPTIONS_DELIMITER)) :
                "";
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
        return databaseVendor.databaseType().toUpperCase(Locale.ROOT);
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

    public RdsViewDialect getRdsViewDialect() {
        return rdsViewDialect;
    }

    @Override
    public String toString() {
        return "RdsView{" +
                "connectionURL='" + connectionURL + '\'' +
                ", useSsl=" + useSsl +
                ", sslCertificateFilePath='" + sslCertificateFilePath + '\'' +
                ", connectionDriver='" + connectionDriver + '\'' +
                ", connectionUserName='" + connectionUserName + '\'' +
                ", databaseName='" + databaseName + '\'' +
                ", host='" + host + '\'' +
                ", hostWithPortWithJdbc='" + hostWithPortWithJdbc + '\'' +
                ", subprotocol='" + subprotocol + '\'' +
                ", connectionString='" + connectionString + '\'' +
                ", port='" + port + '\'' +
                ", databaseVendor=" + databaseVendor +
                ", withoutJDBCPrefix='" + withoutJDBCPrefix + '\'' +
                ", rdsViewDialect=" + rdsViewDialect +
                ", type='" + type + '\'' +
                '}';
    }
}
