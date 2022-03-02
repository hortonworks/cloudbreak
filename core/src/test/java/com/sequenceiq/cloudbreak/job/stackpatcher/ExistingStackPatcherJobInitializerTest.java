package com.sequenceiq.cloudbreak.job.stackpatcher;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.LOGGING_AGENT_AUTO_RESTART;
import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.METERING_AZURE_METADATA;
import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.UNBOUND_RESTART;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.job.stackpatcher.config.ExistingStackPatcherConfig;
import com.sequenceiq.cloudbreak.job.stackpatcher.config.StackPatchTypeConfig;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class ExistingStackPatcherJobInitializerTest {

    private static final String CRN_1 = "crn1";

    private static final String CRN_2 = "crn2";

    @InjectMocks
    private ExistingStackPatcherJobInitializer underTest;

    @Mock
    private ExistingStackPatcherConfig config;

    @Mock
    private ExistingStackPatcherJobService jobService;

    @Mock
    private StackService stackService;

    @Mock
    private JobResource stack1;

    @Mock
    private JobResource stack2;

    @Captor
    private ArgumentCaptor<ExistingStackPatcherJobAdapter> captor;

    @BeforeEach
    void setUp() {
        lenient().when(stack1.getRemoteResourceId()).thenReturn(CRN_1);
        lenient().when(stack2.getRemoteResourceId()).thenReturn(CRN_2);
        lenient().when(stackService.getAllAliveForAutoSync(Set.of(DELETE_COMPLETED, DELETE_IN_PROGRESS, DELETE_FAILED, CREATE_FAILED, CREATE_IN_PROGRESS)))
                .thenReturn(List.of(stack1, stack2));
    }

    @Test
    void emptyEnabled() {
        StackPatchTypeConfig disabledConfig = getConfig(false);
        when(config.getPatchConfigs()).thenReturn(Map.of(UNBOUND_RESTART, disabledConfig));

        underTest.initJobs();

        verifyNoInteractions(jobService);
    }

    @Test
    void multipleEnabled() {
        when(config.getPatchConfigs()).thenReturn(Map.of(
                UNBOUND_RESTART, getConfig(true),
                LOGGING_AGENT_AUTO_RESTART, getConfig(true),
                METERING_AZURE_METADATA, getConfig(false)));

        underTest.initJobs();

        verify(jobService, times(4)).schedule(captor.capture());
        Assertions.assertThat(captor.getAllValues())
                .anyMatch(a -> a.getJobResource().getRemoteResourceId().equals(CRN_1) && a.getStackPatchType().equals(UNBOUND_RESTART))
                .anyMatch(a -> a.getJobResource().getRemoteResourceId().equals(CRN_1) && a.getStackPatchType().equals(LOGGING_AGENT_AUTO_RESTART))
                .anyMatch(a -> a.getJobResource().getRemoteResourceId().equals(CRN_2) && a.getStackPatchType().equals(UNBOUND_RESTART))
                .anyMatch(a -> a.getJobResource().getRemoteResourceId().equals(CRN_2) && a.getStackPatchType().equals(LOGGING_AGENT_AUTO_RESTART));
    }

    private StackPatchTypeConfig getConfig(boolean enabled) {
        StackPatchTypeConfig disabledConfig = new StackPatchTypeConfig();
        disabledConfig.setEnabled(enabled);
        return disabledConfig;
    }

}
