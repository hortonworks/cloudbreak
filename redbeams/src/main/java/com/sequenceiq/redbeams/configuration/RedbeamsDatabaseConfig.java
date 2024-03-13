package com.sequenceiq.redbeams.configuration;

import static com.sequenceiq.cloudbreak.database.DatabaseUtil.createBatchProperties;

import java.sql.SQLException;

import javax.sql.DataSource;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import com.sequenceiq.cloudbreak.common.database.JpaPropertiesFactory;
import com.sequenceiq.cloudbreak.database.DatabaseProperties;

@Configuration
@ConditionalOnProperty(name = "db.enabled", havingValue = "true", matchIfMissing = true)
public class RedbeamsDatabaseConfig {

    @Inject
    private DataSource dataSource;

    @Inject
    private JpaVendorAdapter jpaVendorAdapter;

    @Inject
    private DatabaseProperties databaseProperties;

    @Inject
    private Environment environment;

    @Bean
    @DependsOn("databaseUpMigration")
    public EntityManagerFactory entityManagerFactory() throws SQLException {
        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();

        entityManagerFactory.setPackagesToScan("com.sequenceiq.redbeams", "com.sequenceiq.flow", "com.sequenceiq.cloudbreak.ha",
                "com.sequenceiq.cloudbreak.structuredevent.domain", "com.sequenceiq.cloudbreak.rotation");
        entityManagerFactory.setDataSource(dataSource);

        entityManagerFactory.setJpaVendorAdapter(jpaVendorAdapter);
        entityManagerFactory.setJpaProperties(JpaPropertiesFactory.create(databaseProperties.getHbm2ddlStrategy(), databaseProperties.isDebug(),
                databaseProperties.getSchemaName(), databaseProperties.getCircuitBreakerType(), createBatchProperties(environment),
                databaseProperties.isEnableTransactionInterceptor()));
        entityManagerFactory.afterPropertiesSet();
        return entityManagerFactory.getObject();
    }
}