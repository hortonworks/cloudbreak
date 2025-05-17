package com.sequenceiq.cloudbreak.conf;

import static com.sequenceiq.cloudbreak.database.DatabaseConfig.DATA_SOURCE_POSTFIX;
import static com.sequenceiq.cloudbreak.job.dynamicentitlement.scheduler.DynamicEntitlementRefreshSchedulerFactoryConfig.QUARTZ_DYNAMIC_ENTITLEMENT_REFRESH_PREFIX;
import static com.sequenceiq.cloudbreak.job.instancechecker.scheduler.InstanceCheckerSchedulerFactoryConfig.QUARTZ_INSTANCE_CHECKER_PREFIX;
import static com.sequenceiq.cloudbreak.job.metering.scheduler.MeteringSyncSchedulerFactoryConfig.QUARTZ_METERING_SYNC_PREFIX;

import java.sql.SQLException;
import java.util.Optional;

import javax.sql.DataSource;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.database.DatabaseProperties;
import com.sequenceiq.cloudbreak.database.DatabaseUtil;
import com.sequenceiq.cloudbreak.database.RdsIamAuthenticationTokenProvider;
import com.sequenceiq.cloudbreak.ha.NodeConfig;

@Configuration
@ConditionalOnProperty(name = "db.enabled", havingValue = "true", matchIfMissing = true)
public class AdditionalDatabaseConfig {

    @Value("${quartz.metering.common.threadpool.size:15}")
    private int meteringCommonThreadpoolSize;

    @Value("${quartz.metering.sync.threadpool.size:15}")
    private int meteringSyncThreadpoolSize;

    @Value("${quartz.dynamic-entitlement.threadpool.size:15}")
    private int dynamicEntitlementThreadpoolSize;

    @Inject
    private DatabaseProperties databaseProperties;

    @Inject
    @Named("databaseAddress")
    private String databaseAddress;

    @Inject
    private NodeConfig nodeConfig;

    @Inject
    private RdsIamAuthenticationTokenProvider rdsIamAuthenticationTokenProvider;

    @Bean(name = QUARTZ_INSTANCE_CHECKER_PREFIX + DATA_SOURCE_POSTFIX)
    public DataSource quartzMeteringDataSource() throws SQLException {
        return DatabaseUtil.getDataSource("hikari-quartz-metering-pool", databaseProperties, databaseAddress, nodeConfig,
                Optional.of(meteringCommonThreadpoolSize), rdsIamAuthenticationTokenProvider);
    }

    @Bean(name = QUARTZ_METERING_SYNC_PREFIX + DATA_SOURCE_POSTFIX)
    public DataSource quartzMeteringSyncDataSource() throws SQLException {
        return DatabaseUtil.getDataSource("hikari-quartz-metering-sync-pool", databaseProperties, databaseAddress, nodeConfig,
                Optional.of(meteringSyncThreadpoolSize), rdsIamAuthenticationTokenProvider);
    }

    @Bean(name = QUARTZ_DYNAMIC_ENTITLEMENT_REFRESH_PREFIX + DATA_SOURCE_POSTFIX)
    public DataSource quartzDynamicEntitlementDataSource() throws SQLException {
        return DatabaseUtil.getDataSource("hikari-quartz-dynamic-entitlement-pool", databaseProperties, databaseAddress, nodeConfig,
                Optional.of(dynamicEntitlementThreadpoolSize), rdsIamAuthenticationTokenProvider);
    }
}
