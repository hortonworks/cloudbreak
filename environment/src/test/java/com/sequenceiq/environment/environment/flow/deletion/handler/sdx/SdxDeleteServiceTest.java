package com.sequenceiq.environment.environment.flow.deletion.handler.sdx;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.core.AttemptResults;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
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
    void deleteSdxClustersForEnvironmentNoSdxFound() {
        PollingConfig pollingConfig = getPollingConfig();
        EnvironmentView environment = getEnvironmentView();
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(environment)).thenReturn(Set.of());
        underTest.deleteSdxClustersForEnvironment(pollingConfig, environment, true);
    }

    @Test
    void deleteSdxClustersForEnvironment() {
        PollingConfig pollingConfig = getPollingConfig();
        EnvironmentView environment = getEnvironmentView();
        String sdxCrn1 = "crn:cdp:datalake:us-west-1:tenant:datalake:crn1";
        String sdxCrn2 = "crn:cdp:sdxsvc:us-west-1:tenant:instance:crn2";
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(environment)).thenReturn(Set.of(sdxCrn1, sdxCrn2));
        when(platformAwareSdxConnector.getAttemptResultForDeletion(any(), any())).thenReturn(AttemptResults.finishWith(null));
        underTest.deleteSdxClustersForEnvironment(pollingConfig, environment, true);
        verify(platformAwareSdxConnector).delete(eq(sdxCrn1), eq(true));
        verify(platformAwareSdxConnector).delete(eq(sdxCrn2), eq(true));
        verifyNoMoreInteractions(platformAwareSdxConnector);
    }

    @Test
    void deleteSdxClustersForEnvironmentFail() {
        PollingConfig pollingConfig = getPollingConfig();
        EnvironmentView environment = getEnvironmentView();
        String sdxCrn1 = "crn:cdp:datalake:us-west-1:tenant:datalake:crn1";
        String sdxCrn2 = "crn:cdp:sdxsvc:us-west-1:tenant:instance:crn2";
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(environment)).thenReturn(Set.of(sdxCrn1, sdxCrn2));
        when(platformAwareSdxConnector.getAttemptResultForDeletion(any(), any())).thenReturn(AttemptResults.breakFor(new IllegalStateException()));
        assertThatThrownBy(() -> underTest.deleteSdxClustersForEnvironment(pollingConfig, environment, true))
                .isInstanceOf(UserBreakException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        verify(platformAwareSdxConnector).delete(eq(sdxCrn1), eq(true));
        verify(platformAwareSdxConnector).delete(eq(sdxCrn2), eq(true));
        verifyNoMoreInteractions(platformAwareSdxConnector);
    }

    @Test
    void deleteSdxClustersForEnvironmentWhenSdxIsEmptyButDatalakeNot() {
        PollingConfig pollingConfig = getPollingConfig();
        EnvironmentView environment = getEnvironmentView();
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(environment)).thenReturn(emptySet());
        when(environmentResourceDeletionService.getDatalakeClusterNames(environment)).thenReturn(Set.of("name"));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(stackV4Endpoint.list(anyLong(), anyString(), anyBoolean())).thenReturn(new StackViewV4Responses());
        underTest.deleteSdxClustersForEnvironment(pollingConfig, environment, true);
        verify(stackV4Endpoint).deleteInternal(eq(0L), eq("name"), eq(true), nullable(String.class));
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
        PollingConfig pollingConfig = PollingConfig.builder()
                .withSleepTime(0)
                .withSleepTimeUnit(TimeUnit.SECONDS)
                .withTimeout(10)
                .withTimeoutTimeUnit(TimeUnit.SECONDS)
                .build();
        return pollingConfig;
    }
}
