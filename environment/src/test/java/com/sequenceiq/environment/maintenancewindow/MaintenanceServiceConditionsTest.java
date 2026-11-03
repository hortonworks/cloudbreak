package com.sequenceiq.environment.maintenancewindow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.sequenceiq.maintenance.api.internal.schedule.endpoint.MaintenanceWindowScheduleInternalEndpoint;
import com.sequenceiq.maintenance.api.v1.schedule.endpoint.MaintenanceWindowScheduleEndpoint;

class MaintenanceServiceConditionsTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> {
                context.getBeanFactory().registerSingleton(
                        "maintenanceWindowScheduleEndpoint", mock(MaintenanceWindowScheduleEndpoint.class));
                context.getBeanFactory().registerSingleton(
                        "maintenanceWindowScheduleInternalEndpoint", mock(MaintenanceWindowScheduleInternalEndpoint.class));
            })
            .withUserConfiguration(EnvironmentMaintenanceWindowScheduleService.class);

    @Test
    void serviceIsAbsentWhenUrlPropertyIsUnset() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(EnvironmentMaintenanceWindowScheduleService.class));
    }

    @Test
    void serviceIsPresentWhenUrlPropertyIsConfigured() {
        contextRunner
                .withPropertyValues(MaintenanceServiceConditions.URL_PROPERTY + "=http://localhost:8080")
                .run(context -> assertThat(context).hasSingleBean(EnvironmentMaintenanceWindowScheduleService.class));
    }
}
