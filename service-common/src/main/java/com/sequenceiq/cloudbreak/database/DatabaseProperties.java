package com.sequenceiq.cloudbreak.database;

import com.sequenceiq.cloudbreak.common.tx.CircuitBreakerType;

public interface DatabaseProperties {

    String getUser();

    String getPassword();

    String getDatabase();

    int getPoolSize();

    long getConnectionTimeout();

    int getMinimumIdle();

    long getIdleTimeout();

    String getSchemaName();

    boolean isSsl();

    String getCertFile();

    String getHbm2ddlStrategy();

    boolean isDebug();

    CircuitBreakerType getCircuitBreakerType();

    boolean isEnableTransactionInterceptor();

    String getDatabaseHost();

    String getDatabasePort();

    String getDatabaseId();

    boolean rdsIamRoleBasedAuthentication();
}
