package com.sequenceiq.cloudbreak.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.tx.CircuitBreakerType;
import com.sequenceiq.cloudbreak.database.DatabaseProperties;
import com.sequenceiq.cloudbreak.database.DatabaseUtil;

@Component
public class CloudbreakDatabaseProperties implements DatabaseProperties {

    @Value("${cb.db.env.user:}")
    private String dbUser;

    @Value("${cb.db.env.pass:}")
    private String dbPassword;

    @Value("${cb.db.env.db:}")
    private String dbName;

    @Value("${cb.db.env.poolsize:60}")
    private int poolSize;

    @Value("${cb.db.env.connectiontimeout:30}")
    private long connectionTimeout;

    @Value("${cb.db.env.minidle:2}")
    private int minimumIdle;

    @Value("${cb.db.env.idletimeout:10}")
    private long idleTimeout;

    @Value("${cb.db.env.schema:" + DatabaseUtil.DEFAULT_SCHEMA_NAME + '}')
    private String dbSchemaName;

    @Value("${cb.db.env.ssl:}")
    private boolean ssl;

    @Value("#{'${cb.cert.dir:}/${cb.db.env.cert.file:}'}")
    private String certFile;

    @Value("${cb.hbm2ddl.strategy:validate}")
    private String hbm2ddlStrategy;

    @Value("${cb.hibernate.debug:false}")
    private boolean debug;

    @Value("${cb.hibernate.circuitbreaker:LOG}")
    private CircuitBreakerType circuitBreakerType;

    @Value("${cb.hibernate.transaction.interceptor:true}")
    private boolean enableTransactionInterceptor;

    @Value("${cb.db.port.5432.tcp.addr:}")
    private String dbHost;

    @Value("${cb.db.port.5432.tcp.port:}")
    private String dbPort;

    @Value("${cb.db.serviceid:}")
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
