package com.sequenceiq.environment.environment.flow.deletion.handler.sdx;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.UserBreakException;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.util.PollingConfig;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@ExtendWith(MockitoExtension.class)
class SdxDeleteServiceTest {

    @Mock
    private SdxEndpoint sdxEndpoint;

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
        when(sdxEndpoint.list(any())).thenReturn(List.of());
        underTest.deleteSdxClustersForEnvironment(pollingConfig, environment);
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
        SdxClusterResponse sdx2 = new SdxClusterResponse();
        sdx2.setCrn("crn2");
        when(sdxEndpoint.list(any())).thenReturn(List.of(sdx1, sdx2), List.of(sdx1), List.of());
        underTest.deleteSdxClustersForEnvironment(pollingConfig, environment);
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
        SdxClusterResponse sdx1 = new SdxClusterResponse();
        sdx1.setCrn("crn1");
        SdxClusterResponse sdx2 = new SdxClusterResponse();
        sdx2.setCrn("crn2");
        sdx2.setStatus(SdxClusterStatusResponse.DELETE_FAILED);
        when(sdxEndpoint.list(any())).thenReturn(List.of(sdx1, sdx2), List.of(sdx2), List.of(sdx2));
        assertThatThrownBy(() -> underTest.deleteSdxClustersForEnvironment(pollingConfig, environment))
                .isInstanceOf(UserBreakException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        verify(sdxEndpoint).deleteByCrn(eq("crn1"), eq(true));
        verify(sdxEndpoint).deleteByCrn(eq("crn2"), eq(true));
        verifyNoMoreInteractions(sdxEndpoint);
    }
}
