package com.sequenceiq.cloudbreak.conf;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.postgresql.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringUtils;

@Configuration
@EnableTransactionManagement
public class DatabaseConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${cb.db.env.user:}")
    private String dbUser;

    @Value("${cb.db.env.pass:}")
    private String dbPassword;

    @Value("${cb.db.env.db:}")
    private String dbName;

    @Value("${cb.db.env.schema:}")
    private String dbSchemaName;

    @Value("${cb.hbm2ddl.strategy:validate}")
    private String hbm2ddlStrategy;

    @Value("${cb.hibernate.debug:false}")
    private boolean debug;

    @Inject
    @Named("databaseAddress")
    private String databaseAddress;

    @Bean
    public DataSource dataSource() {
        createSchemaIfNeeded();
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(Driver.class);
        dataSource.setUrl(createDbUrl());
        dataSource.setUsername(dbUser);
        dataSource.setPassword(dbPassword);
        return dataSource;
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
        jpaTransactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        jpaTransactionManager.afterPropertiesSet();
        return jpaTransactionManager;
    }

    @Bean
    public EntityManager entityManager(EntityManagerFactory entityManagerFactory) {
        return entityManagerFactory.createEntityManager();
    }

    @Bean
    @DependsOn("databaseUpMigration")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();

        entityManagerFactory.setPackagesToScan("com.sequenceiq.cloudbreak.domain");
        entityManagerFactory.setDataSource(dataSource());

        entityManagerFactory.setJpaVendorAdapter(jpaVendorAdapter());
        entityManagerFactory.setJpaProperties(jpaProperties());
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

    private Properties jpaProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", hbm2ddlStrategy);
        properties.setProperty("hibernate.show_sql", Boolean.toString(debug));
        properties.setProperty("hibernate.format_sql", Boolean.toString(debug));
        properties.setProperty("hibernate.use_sql_comments", Boolean.toString(debug));
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("hibernate.default_schema", StringUtils.isEmpty(dbSchemaName) ? "public" : dbSchemaName);
        return properties;
    }

    private String createDbUrl() {
        String url;
        if (!StringUtils.isEmpty(dbSchemaName)) {
            url = String.format("jdbc:postgresql://%s/%s?currentSchema=%s", databaseAddress, dbName, dbSchemaName);
        } else {
            url = String.format("jdbc:postgresql://%s/%s", databaseAddress, dbName);
        }
        return url;
    }

    private void createSchemaIfNeeded() {
        if (!StringUtils.isEmpty(dbSchemaName)) {
            try {
                SimpleDriverDataSource ds = new SimpleDriverDataSource();
                ds.setDriverClass(Driver.class);
                ds.setUrl(String.format("jdbc:postgresql://%s/%s", databaseAddress, dbName));
                Connection conn = ds.getConnection(dbUser, dbPassword);
                Statement statement = conn.createStatement();
                statement.execute("CREATE SCHEMA IF NOT EXISTS " + dbSchemaName);
                conn.close();
            } catch (Exception any) {
                LOGGER.error(String.format("Cannot create the %s schema on %s in %s database", dbSchemaName, databaseAddress, dbName), any);
            }
        }
    }
}
