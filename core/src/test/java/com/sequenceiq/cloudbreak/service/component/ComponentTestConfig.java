package com.sequenceiq.cloudbreak.service.component;

import static com.sequenceiq.cloudbreak.service.component.AwsGp2ToGp3PatchServiceComponentTest.POSTGRES_CONTAINER;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeIopsCalculator;
import com.sequenceiq.cloudbreak.cloud.handler.testcontext.TestApplicationContext;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stackpatch.AwsGp2ToGp3PatchService;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.TenantRepository;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceRepository;

@EnableAutoConfiguration
@EntityScan(basePackages = {
        "com.sequenceiq.cloudbreak.domain",
        "com.sequenceiq.cloudbreak.workspace.model",
        "com.sequenceiq.flow.domain"
})
// StackRepository and CloudbreakFlowLogRepository sit in the same package; register that package and map FlowLog as an entity.
@EnableJpaRepositories(
        basePackageClasses = {
                StackRepository.class,
                ResourceRepository.class,
                TenantRepository.class,
                WorkspaceRepository.class
        },
        repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
@EnableTransactionManagement
@EnableScheduling
// Register only the patch under test; other classes in stackpatch pull in large dependency graphs.
// ResourceService + ResourceToCloudResourceConverter are included for tests that need a real ResourceService (JPA-backed).
@ComponentScan(
        basePackageClasses = {
                AwsGp2ToGp3PatchService.class,
                ResourceService.class,
                ResourceToCloudResourceConverter.class,
                StackUtil.class,
                ResourceAttributeUtil.class,
                StackStatusService.class,
                AwsVolumeIopsCalculator.class
        },
        useDefaultFilters = false,
        includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                AwsGp2ToGp3PatchService.class,
                ResourceService.class,
                ResourceToCloudResourceConverter.class,
                StackUtil.class,
                ResourceAttributeUtil.class,
                StackStatusService.class,
                AwsVolumeIopsCalculator.class
        }),
        excludeFilters = {
                @Filter(
                        type = FilterType.REGEX,
                        pattern = {
                                ".*Test\\$.*",
                                ".*Tests\\$.*"
                        }),
                @Filter(type = FilterType.ANNOTATION, classes = TestConfiguration.class),
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TestApplicationContext.class)
        })
public class ComponentTestConfig {

    static class TestEnvironmentInitializer implements ApplicationContextInitializer<GenericApplicationContext> {
        public void initialize(GenericApplicationContext context) {
            TestPropertyValues.of(
                    "spring.jpa.hibernate.ddl-auto=create",
                    "management.endpoint.health.group.readiness.include=readinessState",
                    "logging.level.=ERROR",
                    "spring.datasource.url=" + POSTGRES_CONTAINER.getJdbcUrl(),
                    "spring.datasource.username=" + POSTGRES_CONTAINER.getUsername(),
                    "spring.datasource.password=" + POSTGRES_CONTAINER.getPassword(),
                    "cb.db.port.5432.tcp.addr=" + POSTGRES_CONTAINER.getHost(),
                    "cb.db.port.5432.tcp.port=" + POSTGRES_CONTAINER.getMappedPort(5432),
                    "cb.db.env.user=" + POSTGRES_CONTAINER.getUsername(),
                    "cb.db.env.pass=" + POSTGRES_CONTAINER.getPassword(),
                    "cb.db.env.db=" + POSTGRES_CONTAINER.getDatabaseName(),
                    "spring.quartz.auto-startup=false",
                    "vault.root.token=component-test-token",
                    "statuschecker.enabled=false",
                    "instance.node.id=aaa",
                    "instance.uuid=aaa"
            ).applyTo(context.getEnvironment());
        }
    }
}
