package com.sequenceiq.environment.environment.sync;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

import io.opentracing.Tracer;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EnvironmentStatusTest.TestAppContext.class)
class EnvironmentStatusTest {

    private static final Long ENVIRONMENT_ID = 123L;

    @Inject
    private EnvironmentStatusCheckerJob underTest;

    @MockBean
    private FlowLogService flowLogService;

    @MockBean
    private EnvironmentService environmentService;

    @MockBean
    private FreeIpaService freeIpaService;

    @MockBean
    private EnvironmentStatusUpdateService environmentStatusUpdateService;

    @MockBean
    private Tracer tracer;

    @MockBean
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Mock
    private JobExecutionContext jobExecutionContext;

    private Environment environment;

    @BeforeEach
    void setUp() {
        underTest.setLocalId(ENVIRONMENT_ID.toString());

        environment = new Environment();
        environment.setId(ENVIRONMENT_ID);
        environment.setCreator("creator");
        environment.setResourceCrn("crn:env");
        environment.setAccountId("cloudera");

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        when(flowLogService.isOtherFlowRunning(ENVIRONMENT_ID)).thenReturn(false);
    }

    @Test
    @DisplayName(
            "GIVEN an available environment " +
            "WHEN FreeIpa is available " +
            "THEN environment status should not be updated"
    )
    void available() throws JobExecutionException {
        environment.setStatus(EnvironmentStatus.AVAILABLE);
        setFreeIpaStatus(Status.AVAILABLE);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.executeTracedJob(jobExecutionContext);

        verify(environmentStatusUpdateService, never()).updateEnvironmentStatusAndNotify(eq(environment), any(), any());
    }

    @Test
    @DisplayName(
            "GIVEN an available environment " +
            "WHEN FreeIpa is deleted on provider side " +
            "THEN environment status should be updated"
    )
    void deleted() throws JobExecutionException {
        environment.setStatus(EnvironmentStatus.AVAILABLE);
        setFreeIpaStatus(Status.DELETED_ON_PROVIDER_SIDE);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.executeTracedJob(jobExecutionContext);

        verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                environment,
                EnvironmentStatus.FREEIPA_DELETED_ON_PROVIDER_SIDE,
                ResourceEvent.ENVIRONMENT_SYNC_FINISHED);
    }

    private void setFreeIpaStatus(Status freeIpaStatus) {
        DescribeFreeIpaResponse freeIpaResponse = new DescribeFreeIpaResponse();
        freeIpaResponse.setStatus(freeIpaStatus);
        when(freeIpaService.internalDescribe(environment.getResourceCrn(), environment.getAccountId())).thenReturn(Optional.of(freeIpaResponse));
    }

    @Configuration
    @Import({
            EnvironmentStatusCheckerJob.class,
            EnvironmentSyncService.class,
            AutoSyncConfig.class
    })
    @PropertySource("classpath:application.yml")
    static class TestAppContext {

        @MockBean
        private EnvironmentJobService environmentJobService;

    }
}
