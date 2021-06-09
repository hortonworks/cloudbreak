package com.sequenceiq.cloudbreak.common.database;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.tx.CircuitBreakerType;
import com.sequenceiq.cloudbreak.common.tx.HibernateNPlusOneCircuitBreaker;
import com.sequenceiq.cloudbreak.common.tx.HibernateNPlusOneLogger;

public class JpaPropertiesFacory {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaPropertiesFacory.class);

    private JpaPropertiesFacory() {
    }

    public static Properties create(String hbm2ddlStrategy, boolean debug, String dbSchemaName, CircuitBreakerType circuitBreakerType,
            BatchProperties batchProperties) {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", hbm2ddlStrategy);
        properties.setProperty("hibernate.show_sql", Boolean.toString(debug));
        properties.setProperty("hibernate.format_sql", Boolean.toString(debug));
        properties.setProperty("hibernate.use_sql_comments", Boolean.toString(debug));
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("hibernate.default_schema", dbSchemaName);
        properties.setProperty("hibernate.jdbc.lob.non_contextual_creation", Boolean.toString(true));
        if (batchProperties.getBatchSize() != null) {
            properties.setProperty("hibernate.jdbc.batch_size", Integer.toString(batchProperties.getBatchSize()));
        }
        if (batchProperties.getOrderInserts() != null) {
            properties.setProperty("hibernate.order_inserts", Boolean.toString(batchProperties.getOrderInserts()));
        }
        if (batchProperties.getOrderUpdates() != null) {
            properties.setProperty("hibernate.order_updates", Boolean.toString(batchProperties.getOrderUpdates()));
        }
        if (batchProperties.getBatchVersionedData() != null) {
            properties.setProperty("hibernate.jdbc.batch_versioned_data", Boolean.toString(batchProperties.getBatchVersionedData()));
        }

        LOGGER.info("Hibernate NPlusOne Circuit Breaker type: {}", circuitBreakerType);
        switch (circuitBreakerType) {
            case LOG:
                properties.setProperty("hibernate.session.events.auto", HibernateNPlusOneLogger.class.getName());
                break;
            case BREAK:
                LOGGER.warn("Hibernate NPlusOne Circuit Breaker has been enabled!");
                properties.setProperty("hibernate.session.events.auto", HibernateNPlusOneCircuitBreaker.class.getName());
                break;
            default:
                LOGGER.info("Hibernate NPlusOne Circuit Breaker is disabled!");
        }
        LOGGER.info("JPA Properties: {}", properties);
        return properties;
    }
}
