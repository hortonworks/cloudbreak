package com.sequenceiq.cloudbreak.maintenancewindow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.maintenance.api.internal.schedule.endpoint.MaintenanceWindowScheduleInternalEndpoint;
import com.sequenceiq.maintenance.api.v1.task.endpoint.MaintenanceWindowTaskEndpoint;

/**
 * Guards the single source of truth for the maintenance-service gate. Every bean conditioned on
 * {@link MaintenanceServiceConditions#URL_CONFIGURED} must appear only when cb.maintenance.url is set,
 * and the failure mode is silent (no bean, no error), so it is worth asserting both directions.
 */
class MaintenanceServiceConditionsTest {

    /**
     * The collaborators are registered as ready-made singletons rather than via @Bean methods: StackDtoService is a
     * @Component with @Inject fields, so a mock of it created through the normal bean lifecycle would have those
     * fields autowired and would drag in most of core's graph. registerSingleton skips that post-processing.
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> {
                context.getBeanFactory().registerSingleton("maintenanceWindowScheduleInternalEndpoint",
                        mock(MaintenanceWindowScheduleInternalEndpoint.class));
                context.getBeanFactory().registerSingleton("maintenanceWindowTaskEndpoint",
                        mock(MaintenanceWindowTaskEndpoint.class));
                context.getBeanFactory().registerSingleton("stackDtoService", mock(StackDtoService.class));
            })
            .withUserConfiguration(MaintenanceWindowScheduleLookupService.class,
                    MaintenanceWindowSecretRotationTaskRegistrar.class);

    @Test
    void servicesAreAbsentWhenUrlPropertyIsUnset() {
        contextRunner.run(context -> assertThat(context)
                .doesNotHaveBean(MaintenanceWindowScheduleLookupService.class)
                .doesNotHaveBean(MaintenanceWindowSecretRotationTaskRegistrar.class));
    }

    @Test
    void servicesAreAbsentWhenUrlPropertyIsEmpty() {
        contextRunner
                .withPropertyValues(MaintenanceServiceConditions.URL_PROPERTY + "=")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(MaintenanceWindowScheduleLookupService.class)
                        .doesNotHaveBean(MaintenanceWindowSecretRotationTaskRegistrar.class));
    }

    @Test
    void servicesAreAbsentWhenUrlPropertyIsBlank() {
        contextRunner
                .withPropertyValues(MaintenanceServiceConditions.URL_PROPERTY + "=   ")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(MaintenanceWindowScheduleLookupService.class)
                        .doesNotHaveBean(MaintenanceWindowSecretRotationTaskRegistrar.class));
    }

    @Test
    void servicesArePresentWhenUrlPropertyIsConfigured() {
        contextRunner
                .withPropertyValues(MaintenanceServiceConditions.URL_PROPERTY + "=http://localhost:8080")
                .run(context -> assertThat(context)
                        .hasSingleBean(MaintenanceWindowScheduleLookupService.class)
                        .hasSingleBean(MaintenanceWindowSecretRotationTaskRegistrar.class));
    }
}
