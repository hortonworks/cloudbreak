package com.sequenceiq.externalizedcompute.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.tx.CircuitBreakerType;
import com.sequenceiq.cloudbreak.database.DatabaseProperties;
import com.sequenceiq.cloudbreak.database.DatabaseUtil;

@Component
public class ExternalizedComputeDatabaseProperties implements DatabaseProperties {

    @Value("${externalizedcompute.db.env.user:}")
    private String dbUser;

    @Value("${externalizedcompute.db.env.pass:}")
    private String dbPassword;

    @Value("${externalizedcompute.db.env.db:}")
    private String dbName;

    @Value("${externalizedcompute.db.env.poolsize:10}")
    private int poolSize;

    @Value("${externalizedcompute.db.env.connectiontimeout:30}")
    private long connectionTimeout;

    @Value("${externalizedcompute.db.env.minidle:2}")
    private int minimumIdle;

    @Value("${externalizedcompute.db.env.idletimeout:10}")
    private long idleTimeout;

    @Value("${externalizedcompute.db.env.schema:" + DatabaseUtil.DEFAULT_SCHEMA_NAME + '}')
    private String dbSchemaName;

    @Value("${externalizedcompute.db.env.ssl:}")
    private boolean ssl;

    @Value("${externalizedcompute.db.env.rdsiamrolebasedauthentication:false}")
    private boolean rdsIamBasedAuthEnabled;

    @Value("#{'${externalizedcompute.cert.dir:}/${externalizedcompute.db.env.cert.file:}'}")
    private String certFile;

    @Value("${externalizedcompute.hbm2dexternalizedcompute.strategy:validate}")
    private String hbm2ddlStrategy;

    @Value("${externalizedcompute.hibernate.debug:false}")
    private boolean debug;

    @Value("${externalizedcompute.hibernate.circuitbreaker:LOG}")
    private CircuitBreakerType circuitBreakerType;

    @Value("${externalizedcompute.hibernate.transaction.interceptor:true}")
    private boolean enableTransactionInterceptor;

    @Value("${externalizedcompute.db.port.5432.tcp.addr:}")
    private String dbHost;

    @Value("${externalizedcompute.db.port.5432.tcp.port:}")
    private String dbPort;

    @Value("${externalizedcompute.db.serviceid:}")
    private String databaseId;

    @Override
    public String getUser() {
        return dbUser;
    }

    @Override
    public String getPassword() {
        return dbPassword;
    }

    @Override
    public String getDatabase() {
        return dbName;
    }

    @Override
    public int getPoolSize() {
        return poolSize;
    }

    @Override
    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    @Override
    public int getMinimumIdle() {
        return minimumIdle;
    }

    @Override
    public long getIdleTimeout() {
        return idleTimeout;
    }

    @Override
    public String getSchemaName() {
        return dbSchemaName;
    }

    @Override
    public boolean isSsl() {
        return ssl;
    }

    @Override
    public String getCertFile() {
        return certFile;
    }

    @Override
    public String getHbm2ddlStrategy() {
        return hbm2ddlStrategy;
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    @Override
    public CircuitBreakerType getCircuitBreakerType() {
        return circuitBreakerType;
    }

    @Override
    public boolean isEnableTransactionInterceptor() {
        return enableTransactionInterceptor;
    }

    @Override
    public String getDatabaseHost() {
        return dbHost;
    }

    @Override
    public String getDatabasePort() {
        return dbPort;
    }

    @Override
    public String getDatabaseId() {
        return databaseId;
    }

    @Override
    public boolean rdsIamRoleBasedAuthentication() {
        return rdsIamBasedAuthEnabled;
    }
}
