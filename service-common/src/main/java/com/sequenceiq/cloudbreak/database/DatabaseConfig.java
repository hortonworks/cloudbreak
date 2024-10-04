package com.sequenceiq.cloudbreak.database;

import static com.sequenceiq.cloudbreak.database.DatabaseUtil.createBatchProperties;

import java.sql.SQLException;
import java.util.Optional;

import javax.sql.DataSource;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.sequenceiq.cloudbreak.common.database.JpaPropertiesFactory;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.SchedulerFactoryConfig;

@Configuration
@EnableTransactionManagement
@ConditionalOnProperty(name = "db.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseConfig {

    public static final String DATA_SOURCE_POSTFIX = "DataSource";

    public static final String DEFAULT_DATA_SOURCE_PREFIX = "default";

    public static final String DEFAULT_DATA_SOURCE = DEFAULT_DATA_SOURCE_PREFIX + DATA_SOURCE_POSTFIX;

    @Value("${quartz.default.threadpool.size:15}")
    private int quartzThreadpoolSize;

    @Inject
    private DatabaseProperties databaseProperties;

    @Inject
    private NodeConfig nodeConfig;

    @Inject
    private Environment environment;

    @Inject
    @Named("databaseAddress")
    private String databaseAddress;

    @Inject
    private RdsIamAuthenticationTokenProvider rdsIamAuthenticationTokenProvider;

    @Bean(name = DEFAULT_DATA_SOURCE)
    public DataSource defaultDataSource() throws SQLException {
        return DatabaseUtil.getDataSource("hikari-app-pool", databaseProperties, databaseAddress, nodeConfig, rdsIamAuthenticationTokenProvider);
    }

    @Bean(name = SchedulerFactoryConfig.QUARTZ_PREFIX + DATA_SOURCE_POSTFIX)
    public DataSource quartzDataSource() throws SQLException {
        return DatabaseUtil.getDataSource("hikari-quartz-pool", databaseProperties, databaseAddress, nodeConfig, Optional.of(quartzThreadpoolSize),
                rdsIamAuthenticationTokenProvider);
    }

    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource() {
        return new RoutingDataSource();
    }

    @Bean
    public PlatformTransactionManager transactionManager() throws SQLException {
        JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
        jpaTransactionManager.setEntityManagerFactory(entityManagerFactory());
        jpaTransactionManager.afterPropertiesSet();
        return jpaTransactionManager;
    }

    @Bean
    @DependsOn("databaseUpMigration")
    @ConditionalOnMissingBean(EntityManagerFactory.class)
    public EntityManagerFactory entityManagerFactory() throws SQLException {
        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();

        entityManagerFactory.setPackagesToScan("com.sequenceiq");
        entityManagerFactory.setDataSource(dataSource());

        entityManagerFactory.setJpaVendorAdapter(jpaVendorAdapter());
        entityManagerFactory.setJpaProperties(JpaPropertiesFactory.create(databaseProperties.getHbm2ddlStrategy(), databaseProperties.isDebug(),
                databaseProperties.getSchemaName(), databaseProperties.getCircuitBreakerType(), createBatchProperties(environment),
                databaseProperties.isEnableTransactionInterceptor()));
        entityManagerFactory.afterPropertiesSet();
        return entityManagerFactory.getObject();
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
        hibernateJpaVendorAdapter.setShowSql(true);
        hibernateJpaVendorAdapter.setDatabase(Database.POSTGRESQL);
        return hibernateJpaVendorAdapter;
    }
}
