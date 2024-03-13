package com.sequenceiq.freeipa.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.tx.CircuitBreakerType;
import com.sequenceiq.cloudbreak.database.DatabaseProperties;
import com.sequenceiq.cloudbreak.database.DatabaseUtil;

@Component
public class FreeIpaDatabaseProperties implements DatabaseProperties {

    @Value("${freeipa.db.env.user:}")
    private String dbUser;

    @Value("${freeipa.db.env.pass:}")
    private String dbPassword;

    @Value("${freeipa.db.env.db:}")
    private String dbName;

    @Value("${freeipa.db.env.poolsize:30}")
    private int poolSize;

    @Value("${freeipa.db.env.connectiontimeout:30}")
    private long connectionTimeout;

    @Value("${freeipa.db.env.minidle:2}")
    private int minimumIdle;

    @Value("${freeipa.db.env.idletimeout:10}")
    private long idleTimeout;

    @Value("${freeipa.db.env.schema:" + DatabaseUtil.DEFAULT_SCHEMA_NAME + '}')
    private String dbSchemaName;

    @Value("${freeipa.db.env.ssl:}")
    private boolean ssl;

    @Value("#{'${freeipa.cert.dir:}/${freeipa.db.env.cert.file:}'}")
    private String certFile;

    @Value("${freeipa.hbm2dfreeipa.strategy:validate}")
    private String hbm2ddlStrategy;

    @Value("${freeipa.hibernate.debug:false}")
    private boolean debug;

    @Value("${freeipa.hibernate.circuitbreaker:LOG}")
    private CircuitBreakerType circuitBreakerType;

    @Value("${freeipa.hibernate.transaction.interceptor:true}")
    private boolean enableTransactionInterceptor;

    @Value("${freeipa.db.addr:}")
    private String dbHost;

    @Value("${freeipa.db.port:}")
    private String dbPort;

    @Value("${freeipa.db.serviceid:}")
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

}
