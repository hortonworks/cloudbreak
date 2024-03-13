package com.sequenceiq.environment.configuration;

import static com.sequenceiq.cloudbreak.database.DatabaseUtil.DEFAULT_SCHEMA_NAME;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.tx.CircuitBreakerType;
import com.sequenceiq.cloudbreak.database.DatabaseProperties;

@Component
public class EnvironmentDatabaseProperties implements DatabaseProperties {

    @Value("${environment.db.env.user:}")
    private String dbUser;

    @Value("${environment.db.env.pass:}")
    private String dbPassword;

    @Value("${environment.db.env.db:}")
    private String dbName;

    @Value("${environment.db.env.poolsize:10}")
    private int poolSize;

    @Value("${environment.db.env.connectiontimeout:30}")
    private long connectionTimeout;

    @Value("${environment.db.env.minidle:2}")
    private int minimumIdle;

    @Value("${environment.db.env.idletimeout:10}")
    private long idleTimeout;

    @Value("${environment.db.env.schema:" + DEFAULT_SCHEMA_NAME + '}')
    private String dbSchemaName;

    @Value("${environment.db.env.ssl:}")
    private boolean ssl;

    @Value("#{'${environment.cert.dir:}/${environment.db.env.cert.file:}'}")
    private String certFile;

    @Value("${environment.hbm2d.strategy:validate}")
    private String hbm2ddlStrategy;

    @Value("${environment.hibernate.debug:false}")
    private boolean debug;

    @Value("${environment.hibernate.circuitbreaker:LOG}")
    private CircuitBreakerType circuitBreakerType;

    @Value("${environment.hibernate.transaction.interceptor:true}")
    private boolean enableTransactionInterceptor;

    @Value("${environment.db.host:}")
    private String dbHost;

    @Value("${environment.db.port:}")
    private String dbPort;

    @Value("${environment.db.serviceId:}")
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
