package com.sequenceiq.periscope.config;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.postgresql.Driver;
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

import com.sequenceiq.cloudbreak.util.DatabaseUtil;

@Configuration
@EnableTransactionManagement
public class DatabaseConfig {
    @Value("${periscope.db.env.user:postgres}")
    private String dbUser;

    @Value("${periscope.db.env.pass:}")
    private String dbPassword;

    @Value("${periscope.db.env.db:periscopedb}")
    private String dbName;

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

    @Inject
    @Named("databaseAddress")
    private String databaseAddress;

    @Bean
    public DataSource dataSource() throws SQLException {
        DatabaseUtil.createSchemaIfNeeded("postgresql", databaseAddress, dbName, dbUser, dbPassword, dbSchemaName);
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(Driver.class);
        if (ssl && Files.exists(Paths.get(certFile))) {
            Properties properties = new Properties();
            properties.setProperty("ssl", "true");
            properties.setProperty("sslfactory", "org.postgresql.ssl.SingleCertValidatingFactory");
            properties.setProperty("sslfactoryarg", "file://" + certFile);
            dataSource.setConnectionProperties(properties);
        }
        dataSource.setUrl(String.format("jdbc:postgresql://%s/%s?currentSchema=%s", databaseAddress, dbName, dbSchemaName));
        dataSource.setUsername(dbUser);
        dataSource.setPassword(dbPassword);
        return dataSource;
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

        entityManagerFactory.setPackagesToScan("com.sequenceiq.periscope.domain");
        entityManagerFactory.setDataSource(dataSource());

        entityManagerFactory.setJpaVendorAdapter(jpaVendorAdapter());
        entityManagerFactory.setJpaProperties(jpaProperties());
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

    private Properties jpaProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", hbm2ddlStrategy);
        properties.setProperty("hibernate.show_sql", Boolean.toString(debug));
        properties.setProperty("hibernate.format_sql", Boolean.toString(debug));
        properties.setProperty("hibernate.use_sql_comments", Boolean.toString(debug));
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("hibernate.default_schema", dbSchemaName);
        return properties;
    }
}
