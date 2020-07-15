package com.sequenceiq.environment.environment.flow.deletion.handler.sdx;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentResourceDeletionService;
import com.sequenceiq.environment.util.PollingConfig;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@ExtendWith(MockitoExtension.class)
class SdxDeleteServiceTest {

    @Mock
    private SdxEndpoint sdxEndpoint;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private EnvironmentResourceDeletionService environmentResourceDeletionService;

    @InjectMocks
    private SdxDeleteService underTest;

    @BeforeEach
    void setUp() {
    }

    @Test
    void deleteSdxClustersForEnvironmentNoSdxFound() {
        PollingConfig pollingConfig = PollingConfig.builder()
                .withSleepTime(0)
                .withSleepTimeUnit(TimeUnit.SECONDS)
                .withTimeout(0)
                .withTimeoutTimeUnit(TimeUnit.SECONDS)
                .build();
        Environment environment = new Environment();
        environment.setName("envName");
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(environment)).thenReturn(Set.of());
        underTest.deleteSdxClustersForEnvironment(pollingConfig, environment, true);
        verifyNoMoreInteractions(sdxEndpoint);
    }

    @Test
    void deleteSdxClustersForEnvironment() {
        PollingConfig pollingConfig = PollingConfig.builder()
                .withSleepTime(0)
                .withSleepTimeUnit(TimeUnit.SECONDS)
                .withTimeout(10)
                .withTimeoutTimeUnit(TimeUnit.SECONDS)
                .build();
        Environment environment = new Environment();
        environment.setName("envName");
        SdxClusterResponse sdx1 = new SdxClusterResponse();
        sdx1.setCrn("crn1");
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(environment)).thenReturn(Set.of("crn1", "crn2"));
        when(sdxEndpoint.list(environment.getName())).thenReturn(List.of(sdx1)).thenReturn(List.of());
        underTest.deleteSdxClustersForEnvironment(pollingConfig, environment, true);
        verify(sdxEndpoint).deleteByCrn(eq("crn1"), eq(true));
        verify(sdxEndpoint).deleteByCrn(eq("crn2"), eq(true));
        verifyNoMoreInteractions(sdxEndpoint);
    }

    @Test
    void deleteSdxClustersForEnvironmentFail() {
        PollingConfig pollingConfig = PollingConfig.builder()
                .withSleepTime(0)
                .withSleepTimeUnit(TimeUnit.SECONDS)
                .withTimeout(10)
                .withTimeoutTimeUnit(TimeUnit.SECONDS)
                .build();
        Environment environment = new Environment();
        environment.setName("envName");
        SdxClusterResponse sdx2 = new SdxClusterResponse();
        sdx2.setCrn("crn2");
        sdx2.setStatus(SdxClusterStatusResponse.DELETE_FAILED);
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(environment)).thenReturn(Set.of("crn1", "crn2"));
        when(sdxEndpoint.list(environment.getName())).thenReturn(List.of(sdx2));
        assertThatThrownBy(() -> underTest.deleteSdxClustersForEnvironment(pollingConfig, environment, true))
                .isInstanceOf(UserBreakException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        verify(sdxEndpoint).deleteByCrn(eq("crn1"), eq(true));
        verify(sdxEndpoint).deleteByCrn(eq("crn2"), eq(true));
        verifyNoMoreInteractions(sdxEndpoint);
    }

    @Test
    void deleteSdxClustersForEnvironmentWhenSdxIsEmptyButDatalakeNot() {
        PollingConfig pollingConfig = PollingConfig.builder()
                .withSleepTime(0)
                .withSleepTimeUnit(TimeUnit.SECONDS)
                .withTimeout(10)
                .withTimeoutTimeUnit(TimeUnit.SECONDS)
                .build();
        Environment environment = new Environment();
        environment.setName("envName");
        environment.setResourceCrn(Crn.builder().setAccountId("asd")
                .setResource("asd")
                .setResourceType(Crn.ResourceType.ENVIRONMENT)
                .setService(Crn.Service.ENVIRONMENTS)
                .setPartition(Crn.Partition.CDP)
                .build().toString());
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(environment)).thenReturn(emptySet());
        when(environmentResourceDeletionService.getDatalakeClusterNames(environment)).thenReturn(Set.of("name"));
        underTest.deleteSdxClustersForEnvironment(pollingConfig, environment, true);
        verify(stackV4Endpoint).delete(eq(0L), eq("name"), eq(true), anyString());
        verify(sdxEndpoint, times(0)).deleteByCrn(anyString(), anyBoolean());
    }
}
