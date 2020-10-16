package com.sequenceiq.periscope.config;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.sequenceiq.cloudbreak.common.database.JpaPropertiesFacory;
import com.sequenceiq.cloudbreak.common.tx.CircuitBreakerType;
import com.sequenceiq.cloudbreak.util.DatabaseUtil;
import com.sequenceiq.periscope.service.ha.PeriscopeNodeConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EnableTransactionManagement
@ConditionalOnProperty(name = "periscope.db.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseConfig {

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

    @Inject
    @Named("databaseAddress")
    private String databaseAddress;

    @Inject
    private PeriscopeNodeConfig periscopeNodeConfig;

    @Bean
    public DataSource dataSource() throws SQLException {
        DatabaseUtil.createSchemaIfNeeded("postgresql", databaseAddress, dbName, dbUser, dbPassword, dbSchemaName);
        HikariConfig config = new HikariConfig();
        if (ssl && Files.exists(Paths.get(certFile))) {
            config.addDataSourceProperty("ssl", "true");
            config.addDataSourceProperty("sslfactory", "org.postgresql.ssl.SingleCertValidatingFactory");
            config.addDataSourceProperty("sslfactoryarg", "file://" + certFile);
        }
        if (periscopeNodeConfig.isNodeIdSpecified()) {
            config.addDataSourceProperty("ApplicationName", periscopeNodeConfig.getId());
        }
        config.setJdbcUrl(String.format("jdbc:postgresql://%s/%s?currentSchema=%s&traceWithActiveSpanOnly=true", databaseAddress, dbName, dbSchemaName));
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
    public EntityManager entityManager(EntityManagerFactory entityManagerFactory) {
        return entityManagerFactory.createEntityManager();
    }

    @Bean
    @DependsOn("databaseUpMigration")
    public EntityManagerFactory entityManagerFactory() throws SQLException {
        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();

        entityManagerFactory.setPackagesToScan("com.sequenceiq.periscope", "com.sequenceiq.flow");
        entityManagerFactory.setDataSource(dataSource());

        entityManagerFactory.setJpaVendorAdapter(jpaVendorAdapter());
        entityManagerFactory.setJpaProperties(JpaPropertiesFacory.create("hibernate.hbm2ddl.auto", hbm2ddlStrategy,
                debug, dbSchemaName, circuitBreakerType));
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
