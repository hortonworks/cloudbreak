package com.sequenceiq.periscope.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.tx.CircuitBreakerType;
import com.sequenceiq.cloudbreak.database.DatabaseProperties;
import com.sequenceiq.cloudbreak.database.DatabaseUtil;

@Component
public class PeriscopeDatabaseProperties implements DatabaseProperties {

    @Value("${periscope.db.env.user:postgres}")
    private String dbUser;

    @Value("${periscope.db.env.pass:}")
    private String dbPassword;

    @Value("${periscope.db.env.db:periscopedb}")
    private String dbName;

    @Value("${periscope.db.env.poolsize:10}")
    private int poolSize;

    @Value("${periscope.db.env.connectiontimeout:30}")
    private long connectionTimeout;

    @Value("${periscope.db.env.minidle:2}")
    private int minimumIdle;

    @Value("${periscope.db.env.idletimeout:10}")
    private long idleTimeout;

    @Value("${periscope.db.env.schema:" + DatabaseUtil.DEFAULT_SCHEMA_NAME + '}')
    private String dbSchemaName;

    @Value("${periscope.db.env.ssl:}")
    private boolean ssl;

    @Value("#{'${periscope.cert.dir:}/${periscope.db.env.cert.file:}'}")
    private String certFile;

    @Value("${periscope.hbm2ddl.strategy:validate}")
    private String hbm2ddlStrategy;

    @Value("${periscope.hibernate.debug:false}")
    private boolean debug;

    @Value("${periscope.hibernate.circuitbreaker:LOG}")
    private CircuitBreakerType circuitBreakerType;

    @Value("${periscope.hibernate.transaction.interceptor:true}")
    private boolean enableTransactionInterceptor;

    @Value("${periscope.db.port.5432.tcp.addr:}")
    private String dbHost;

    @Value("${periscope.db.port.5432.tcp.port:}")
    private String dbPort;

    @Value("${periscope.db.serviceid:}")
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
