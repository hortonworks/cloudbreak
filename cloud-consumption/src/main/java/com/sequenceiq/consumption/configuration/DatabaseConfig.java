package com.sequenceiq.consumption.configuration;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.postgresql.Driver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
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
import com.sequenceiq.flow.ha.NodeConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EnableTransactionManagement
@ConditionalOnProperty(name = "consumption.db.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseConfig {

    private static final String DEFAULT_SCHEMA_NAME = "public";

    @Value("${consumption.db.env.user:}")
    private String dbUser;

    @Value("${consumption.db.env.pass:}")
    private String dbPassword;

    @Value("${consumption.db.env.db:}")
    private String dbName;

    @Value("${consumption.db.env.poolsize:10}")
    private int poolSize;

    @Value("${consumption.db.env.connectiontimeout:30}")
    private long connectionTimeout;

    @Value("${consumption.db.env.minidle:2}")
    private int minimumIdle;

    @Value("${consumption.db.env.idletimeout:10}")
    private long idleTimeout;

    @Value("${consumption.db.env.schema:" + DEFAULT_SCHEMA_NAME + '}')
    private String dbSchemaName;

    @Value("${consumption.db.env.ssl:}")
    private boolean ssl;

    @Value("#{'${consumption.cert.dir:}/${consumption.db.env.cert.file:}'}")
    private String certFile;

    @Value("${consumption.hbm2d.strategy:validate}")
    private String hbm2ddlStrategy;

    @Value("${consumption.hibernate.debug:false}")
    private boolean debug;

    @Value("${consumption.hibernate.circuitbreaker:LOG}")
    private CircuitBreakerType circuitBreakerType;

    @Value("${consumption.hibernate.transaction.interceptor:true}")
    private boolean enableTransactionInterceptor;

    @Inject
    @Named("databaseAddress")
    private String databaseAddress;

    @Inject
    private NodeConfig nodeConfig;

    @Inject
    private Environment environment;

    @Bean
    public DataSource dataSource() throws SQLException {
        createSchemaIfNeeded("postgresql", databaseAddress, dbName, dbUser, dbPassword, dbSchemaName);
        HikariConfig config = new HikariConfig();
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
        jpaTransactionManager.setEntityManagerFactory(entityManagerFactory());
        jpaTransactionManager.afterPropertiesSet();
        return jpaTransactionManager;
    }

    @Bean
    @DependsOn("databaseUpMigration")
    public EntityManagerFactory entityManagerFactory() throws SQLException {
        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();

        entityManagerFactory.setPackagesToScan("com.sequenceiq");
        entityManagerFactory.setDataSource(dataSource());

        entityManagerFactory.setJpaVendorAdapter(jpaVendorAdapter());
        entityManagerFactory.setJpaProperties(JpaPropertiesFacory.create(hbm2ddlStrategy, debug, dbSchemaName, circuitBreakerType, createBatchProperties(),
                enableTransactionInterceptor));
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

    private void createSchemaIfNeeded(String dbType, String dbAddress, String dbName, String dbUser, String dbPassword, String dbSchema)
            throws SQLException {
        if (!DEFAULT_SCHEMA_NAME.equals(dbSchema)) {
            SimpleDriverDataSource ds = new SimpleDriverDataSource();
            ds.setDriverClass(Driver.class);
            ds.setUrl(String.format("jdbc:%s://%s/%s", dbType, dbAddress, dbName));
            try (Connection conn = ds.getConnection(dbUser, dbPassword); Statement statement = conn.createStatement()) {
                statement.execute("CREATE SCHEMA IF NOT EXISTS " + dbSchema);
            }
        }
    }

    private BatchProperties createBatchProperties() {
        return new BatchProperties(environment.getProperty("spring.jpa.properties.hibernate.jdbc.batch_size", Integer.class),
                environment.getProperty("spring.jpa.properties.hibernate.order_inserts", Boolean.class),
                environment.getProperty("spring.jpa.properties.hibernate.order_updates", Boolean.class),
                environment.getProperty("spring.jpa.properties.hibernate.jdbc.batch_versioned_data", Boolean.class));
    }
}
