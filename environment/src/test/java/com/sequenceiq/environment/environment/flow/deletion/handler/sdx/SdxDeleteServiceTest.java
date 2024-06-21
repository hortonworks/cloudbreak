package com.sequenceiq.environment.environment.flow.deletion.handler.sdx;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.core.AttemptResults;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.service.EnvironmentResourceDeletionService;
import com.sequenceiq.environment.util.PollingConfig;

@ExtendWith(MockitoExtension.class)
class SdxDeleteServiceTest {

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private EnvironmentResourceDeletionService environmentResourceDeletionService;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private SdxDeleteService underTest;

    @Test
    void deleteSdxClustersForEnvironment() {
        PollingConfig pollingConfig = getPollingConfig();
        EnvironmentView environment = getEnvironmentView();
        doNothing().when(platformAwareSdxConnector).deleteByEnvironment(any(), any());
        when(platformAwareSdxConnector.getAttemptResultForDeletion(any())).thenReturn(AttemptResults.finishWith(null));
        underTest.deleteSdxClustersForEnvironment(pollingConfig, environment, true);
        verify(platformAwareSdxConnector).deleteByEnvironment(any(), any());
    }

    @Test
    void deleteSdxClustersForEnvironmentFail() {
        PollingConfig pollingConfig = getPollingConfig();
        EnvironmentView environment = getEnvironmentView();
        doNothing().when(platformAwareSdxConnector).deleteByEnvironment(any(), any());
        when(platformAwareSdxConnector.getAttemptResultForDeletion(any())).thenReturn(AttemptResults.breakFor(new IllegalStateException()));
        assertThatThrownBy(() -> underTest.deleteSdxClustersForEnvironment(pollingConfig, environment, true))
                .isInstanceOf(UserBreakException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        verifyNoMoreInteractions(platformAwareSdxConnector);
        verify(platformAwareSdxConnector).deleteByEnvironment(any(), any());
    }

    private EnvironmentView getEnvironmentView() {
        EnvironmentView environment = new EnvironmentView();
        environment.setName("envName");
        environment.setResourceCrn(CrnTestUtil.getEnvironmentCrnBuilder()
                .setAccountId("asd")
                .setResource("asd")
                .build().toString());
        return environment;
    }

    private PollingConfig getPollingConfig() {
        return PollingConfig.builder()
                .withSleepTime(0)
                .withSleepTimeUnit(TimeUnit.SECONDS)
                .withTimeout(10)
                .withTimeoutTimeUnit(TimeUnit.SECONDS)
                .build();
    }
}
