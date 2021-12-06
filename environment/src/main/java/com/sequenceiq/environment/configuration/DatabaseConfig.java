package com.sequenceiq.environment.configuration;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.nio.file.Files;
import java.nio.file.Paths;
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
import com.sequenceiq.cloudbreak.tracing.TracingConfiguration;
import com.sequenceiq.flow.ha.NodeConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EnableTransactionManagement
@ConditionalOnProperty(name = "environment.db.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseConfig {

    private static final String DEFAULT_SCHEMA_NAME = "public";

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

    @Inject
    @Named("databaseAddress")
    private String databaseAddress;

    @Inject
    private TracingConfiguration tracingConfiguration;

    @Inject
    private NodeConfig nodeConfig;

    @Inject
    private Environment environment;

    @Bean
    public DataSource dataSource() throws SQLException {
        createSchemaIfNeeded("postgresql", databaseAddress, dbName, dbUser, dbPassword, dbSchemaName);
        HikariConfig config = new HikariConfig();
        if (ssl && Files.exists(Paths.get(certFile))) {
            config.addDataSourceProperty("ssl", "true");
            config.addDataSourceProperty("sslfactory", "org.postgresql.ssl.SingleCertValidatingFactory");
            config.addDataSourceProperty("sslfactoryarg", "file://" + certFile);
        }
        if (nodeConfig.isNodeIdSpecified()) {
            config.addDataSourceProperty("ApplicationName", nodeConfig.getId());
        }
        if (tracingConfiguration.isJdbcTracingEnabled()) {
            config.setDriverClassName("io.opentracing.contrib.jdbc.TracingDriver");
        }
        config.setJdbcUrl(String.format("jdbc:tracing:postgresql://%s/%s?currentSchema=%s&traceWithActiveSpanOnly=true", databaseAddress, dbName, dbSchemaName));
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
        entityManagerFactory.setJpaProperties(JpaPropertiesFacory.create(hbm2ddlStrategy, debug, dbSchemaName, circuitBreakerType, createBatchProperties()));
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
