package com.sequenceiq.datalake.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.tx.CircuitBreakerType;
import com.sequenceiq.cloudbreak.database.DatabaseProperties;
import com.sequenceiq.cloudbreak.database.DatabaseUtil;

@Component
public class DatalakeDatabaseProperties implements DatabaseProperties {

    @Value("${datalake.db.env.user:}")
    private String dbUser;

    @Value("${datalake.db.env.pass:}")
    private String dbPassword;

    @Value("${datalake.db.env.db:}")
    private String dbName;

    @Value("${datalake.db.env.poolsize:10}")
    private int poolSize;

    @Value("${datalake.db.env.connectiontimeout:30}")
    private long connectionTimeout;

    @Value("${datalake.db.env.minidle:2}")
    private int minimumIdle;

    @Value("${datalake.db.env.idletimeout:10}")
    private long idleTimeout;

    @Value("${datalake.db.env.schema:" + DatabaseUtil.DEFAULT_SCHEMA_NAME + '}')
    private String dbSchemaName;

    @Value("${datalake.db.env.ssl:}")
    private boolean ssl;

    @Value("${datalake.db.env.rdsiamrolebasedauthentication:false}")
    private boolean rdsIamBasedAuthEnabled;

    @Value("#{'${datalake.cert.dir:}/${datalake.db.env.cert.file:}'}")
    private String certFile;

    @Value("${datalake.hbm2ddatalake.strategy:validate}")
    private String hbm2ddlStrategy;

    @Value("${datalake.hibernate.debug:false}")
    private boolean debug;

    @Value("${datalake.hibernate.circuitbreaker:LOG}")
    private CircuitBreakerType circuitBreakerType;

    @Value("${datalake.hibernate.transaction.interceptor:true}")
    private boolean enableTransactionInterceptor;

    @Value("${datalake.db.port.5432.tcp.addr:}")
    private String dbHost;

    @Value("${datalake.db.port.5432.tcp.port:}")
    private String dbPort;

    @Value("${datalake.db.serviceid:}")
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
