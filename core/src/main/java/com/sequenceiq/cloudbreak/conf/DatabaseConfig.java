package com.sequenceiq.cloudbreak.conf;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.sql.SQLException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
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

import com.sequenceiq.cloudbreak.common.database.BatchProperties;
import com.sequenceiq.cloudbreak.common.database.JpaPropertiesFacory;
import com.sequenceiq.cloudbreak.common.tx.CircuitBreakerType;
import com.sequenceiq.cloudbreak.util.DatabaseUtil;
import com.sequenceiq.flow.ha.NodeConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    @Value("${cb.db.env.user:}")
    private String dbUser;

    @Value("${cb.db.env.pass:}")
    private String dbPassword;

    @Value("${cb.db.env.db:}")
    private String dbName;

    @Value("${cb.db.env.poolsize:30}")
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

    @Inject
    @Named("databaseAddress")
    private String databaseAddress;

    @Inject
    private NodeConfig nodeConfig;

    @Inject
    private Environment environment;

    @Bean(name = "defaultDataSource")
    public DataSource defaultDataSource() throws SQLException {
        return getDataSource("hikari-app-pool");
    }

    @Bean(name = "quartzDataSource")
    public DataSource quartzDataSource() throws SQLException {
        return getDataSource("hikari-quartz-pool");
    }

    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource() {
        return new RoutingDataSource();
    }

    private HikariDataSource getDataSource(String poolName) throws SQLException {
        DatabaseUtil.createSchemaIfNeeded("postgresql", databaseAddress, dbName, dbUser, dbPassword, dbSchemaName);
        HikariConfig config = new HikariConfig();
        config.setPoolName(poolName);
        if (ssl) {
            config.addDataSourceProperty("ssl", "true");
            config.addDataSourceProperty("sslfactory", "org.postgresql.ssl.DefaultJavaSSLFactory");
        }
        if (nodeConfig.isNodeIdSpecified()) {
            config.addDataSourceProperty("ApplicationName", nodeConfig.getId());
        }
        config.setJdbcUrl(String.format("jdbc:postgresql://%s/%s?currentSchema=%s", databaseAddress, dbName, dbSchemaName));
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(SECONDS.toMillis(connectionTimeout));
        config.setIdleTimeout(MINUTES.toMillis(idleTimeout));
        return new HikariDataSource(config);
    }

    @Bean
    public PlatformTransactionManager transactionManager() throws SQLException {
        JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
        jpaTransactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        jpaTransactionManager.afterPropertiesSet();
        return jpaTransactionManager;
    }

    @Bean
    @DependsOn("databaseUpMigration")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() throws SQLException {
        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();

        entityManagerFactory.setPackagesToScan("com.sequenceiq");
        entityManagerFactory.setDataSource(dataSource());

        entityManagerFactory.setJpaVendorAdapter(jpaVendorAdapter());
        entityManagerFactory.setJpaProperties(JpaPropertiesFacory.create(hbm2ddlStrategy, debug, dbSchemaName, circuitBreakerType, createBatchProperties()));
        entityManagerFactory.afterPropertiesSet();
        return entityManagerFactory;
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
        hibernateJpaVendorAdapter.setShowSql(true);
        hibernateJpaVendorAdapter.setDatabase(Database.POSTGRESQL);
        return hibernateJpaVendorAdapter;
    }

    private BatchProperties createBatchProperties() {
        return new BatchProperties(environment.getProperty("spring.jpa.properties.hibernate.jdbc.batch_size", Integer.class),
                environment.getProperty("spring.jpa.properties.hibernate.order_inserts", Boolean.class),
                environment.getProperty("spring.jpa.properties.hibernate.order_updates", Boolean.class),
                environment.getProperty("spring.jpa.properties.hibernate.jdbc.batch_versioned_data", Boolean.class));
    }
}
