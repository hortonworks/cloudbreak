package com.sequenceiq.flow.component;

import static com.sequenceiq.flow.component.FlowComponentTest.POSTGRES_CONTAINER;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.dbmigration.SchemaLocationProvider;
import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.flow.component.sleep.SleepFlowConfig;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.EventParameterFactory;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.flow.core.config.FlowConfiguration;

@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = "com.sequenceiq")
@EnableTransactionManagement
@EnableScheduling
@ComponentScan(basePackages = "com.sequenceiq", excludeFilters = @Filter(
        type = FilterType.REGEX,
        pattern = {
                "com.sequenceiq.authorization.*",
                "com.sequenceiq.cloudbreak.auth.*",
                "com.sequenceiq.cloudbreak.quartz.*",
                "com.sequenceiq.cloudbreak.tracing.*"
        }
))
public class ComponentTestConfig {

    @Bean
    public ApplicationFlowInformation applicationFlowInformation() {
        return new ApplicationFlowInformation() {
            @Override
            public List<Class<? extends FlowConfiguration<?>>> getRestartableFlows() {
                return List.of(SleepFlowConfig.class);
            }

            @Override
            public List<String> getAllowedParallelFlows() {
                return List.of();
            }

            @Override
            public List<Class<? extends FlowConfiguration<?>>> getTerminationFlow() {
                return List.of();
            }
        };
    }

    @Bean
    public PayloadContextProvider payloadContextProvider() {
        return new PayloadContextProvider() {
            @Override
            public PayloadContext getPayloadContext(Long resourceId) {
                return PayloadContext.create(
                        String.format("crn:altus:datalake:us-west-1:datalake:resource:%s", resourceId),
                        String.format("crn:altus:environments:us-west-1:noop:environment:%s", resourceId),
                        "MOCK");
            }
        };
    }

    @Bean
    public ResourceIdProvider resourceIdProvider() {
        AtomicLong idSeq = new AtomicLong(0);
        ConcurrentMap<String, Long> crnIdMap = new ConcurrentHashMap<>();
        ConcurrentMap<String, Long> nameIdMap = new ConcurrentHashMap<>();
        return new ResourceIdProvider() {
            @Override
            public Long getResourceIdByResourceCrn(String resourceCrn) {
                return crnIdMap.computeIfAbsent(resourceCrn, r -> idSeq.incrementAndGet());
            }

            @Override
            public Long getResourceIdByResourceName(String resourceName) {
                return nameIdMap.computeIfAbsent(resourceName, r -> idSeq.incrementAndGet());
            }
        };
    }

    @Bean(name = "conversionService")
    public ConversionServiceFactoryBean conversionServiceFactoryBean() {
        ConversionServiceFactoryBean conversionServiceFactoryBean = new ConversionServiceFactoryBean();
        conversionServiceFactoryBean.afterPropertiesSet();
        return conversionServiceFactoryBean;
    }

    @Bean
    public EventParameterFactory eventParameterFactory() {
        return resourceId -> Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, Objects.requireNonNull(ThreadBasedUserCrnProvider.getUserCrn()));
    }

    @Bean
    @DependsOn("databaseUpMigration")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) throws SQLException {
        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactory.setPackagesToScan("com.sequenceiq");
        entityManagerFactory.setDataSource(dataSource);
        entityManagerFactory.setJpaVendorAdapter(jpaVendorAdapter());
        entityManagerFactory.afterPropertiesSet();
        return entityManagerFactory;
    }

    @Bean
    public SchemaLocationProvider schemaLocationProvider() {
        return new SchemaLocationProvider() {
            @Override
            public Optional<String> pendingSubfolder() {
                return Optional.of("flow_test");
            }

            @Override
            public Optional<String> upSubfolder() {
                return Optional.of("flow");
            }
        };
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
        hibernateJpaVendorAdapter.setDatabase(Database.POSTGRESQL);
        return hibernateJpaVendorAdapter;
    }

    static class TestEnvironmentInitializer implements ApplicationContextInitializer<GenericApplicationContext> {
        public void initialize(GenericApplicationContext context) {
            TestPropertyValues.of(
                    "logging.level.=ERROR",
                    "spring.datasource.url=" + POSTGRES_CONTAINER.getJdbcUrl(),
                    "spring.datasource.username=" + POSTGRES_CONTAINER.getUsername(),
                    "spring.datasource.password=" + POSTGRES_CONTAINER.getPassword(),
                    "opentracing.allowed-header-tags=mytag",
                    "spring.quartz.auto-startup=false",
                    "statuschecker.enabled=false",
                    "instance.node.id=aaa",
                    "instance.uuid=aaa"
            ).applyTo(context.getEnvironment());
        }
    }
}
