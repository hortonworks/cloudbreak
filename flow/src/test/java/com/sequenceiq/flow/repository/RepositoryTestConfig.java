package com.sequenceiq.flow.repository;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = "com.sequenceiq.flow.repository")
@EntityScan(basePackages = "com.sequenceiq.flow.domain")
public class RepositoryTestConfig {

    public static class RepositoryTestInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

        @Override
        public void initialize(GenericApplicationContext applicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=jdbc:h2:mem:test",
                    "spring.jpa.properties.hibernate.show_sql=false",
                    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
            ).applyTo(applicationContext.getEnvironment());
        }
    }
}
