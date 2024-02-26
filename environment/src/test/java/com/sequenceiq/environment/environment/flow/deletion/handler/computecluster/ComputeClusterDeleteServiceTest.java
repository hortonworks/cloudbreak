package com.sequenceiq.environment.environment.flow.deletion.handler.computecluster;

// Required imports for JUnit and Mockito

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.environment.exception.EnvironmentServiceException;
import com.sequenceiq.environment.util.PollingConfig;
import com.sequenceiq.externalizedcompute.api.endpoint.ExternalizedComputeClusterEndpoint;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterApiStatus;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;

@ExtendWith(MockitoExtension.class)
public class ComputeClusterDeleteServiceTest {

    @Mock
    private ExternalizedComputeClusterEndpoint endpoint;

    @InjectMocks
    private ComputeClusterDeleteService computeClusterDeleteService;

    @Test
    public void deleteComputeClusterTest() {
        String envCrn = "envcrn";
        ExternalizedComputeClusterResponse computeClusterResponse1 = new ExternalizedComputeClusterResponse();
        String defaultCluster = "default-cluster";
        computeClusterResponse1.setName(defaultCluster);
        ExternalizedComputeClusterResponse computeClusterResponse2 = new ExternalizedComputeClusterResponse();
        String anotherCluster = "another-cluster";
        computeClusterResponse2.setName(anotherCluster);
        when(endpoint.list(envCrn))
                .thenReturn(List.of(computeClusterResponse1, computeClusterResponse2))
                .thenReturn(List.of(computeClusterResponse1, computeClusterResponse2))
                .thenReturn(List.of());
        ReflectionTestUtils.setField(computeClusterDeleteService, "externalizedComputeEnabled", true);
        PollingConfig pollingConfig = PollingConfig.builder().withSleepTime(0).withTimeout(10).withTimeoutTimeUnit(TimeUnit.SECONDS).build();
        computeClusterDeleteService.deleteComputeCluster(envCrn, pollingConfig);
        verify(endpoint, times(3)).list(envCrn);
        verify(endpoint, times(1)).delete(defaultCluster);
        verify(endpoint, times(1)).delete(anotherCluster);
    }

    @Test
    public void deleteComputeClusterTestWithDeleteFailed() {
        String envCrn = "envcrn";
        ExternalizedComputeClusterResponse computeClusterResponse1 = new ExternalizedComputeClusterResponse();
        String defaultCluster = "default-cluster";
        computeClusterResponse1.setName(defaultCluster);
        computeClusterResponse1.setStatus(ExternalizedComputeClusterApiStatus.AVAILABLE);
        ExternalizedComputeClusterResponse computeClusterResponse2 = new ExternalizedComputeClusterResponse();
        String anotherCluster = "another-cluster";
        computeClusterResponse2.setName(anotherCluster);
        computeClusterResponse2.setStatus(ExternalizedComputeClusterApiStatus.AVAILABLE);

        ExternalizedComputeClusterResponse failedComputeClusterResponse1 = new ExternalizedComputeClusterResponse();
        failedComputeClusterResponse1.setName("failed-cluster");
        failedComputeClusterResponse1.setStatus(ExternalizedComputeClusterApiStatus.DELETE_FAILED);

        when(endpoint.list(envCrn))
                .thenReturn(List.of(computeClusterResponse1, computeClusterResponse2))
                .thenReturn(List.of(failedComputeClusterResponse1));
        ReflectionTestUtils.setField(computeClusterDeleteService, "externalizedComputeEnabled", true);
        PollingConfig pollingConfig = PollingConfig.builder().withSleepTime(0).withTimeout(10).withTimeoutTimeUnit(TimeUnit.SECONDS).build();
        EnvironmentServiceException environmentServiceException = assertThrows(EnvironmentServiceException.class,
                () -> computeClusterDeleteService.deleteComputeCluster(envCrn, pollingConfig));

        assertEquals("Externalized clusters deletion failed. Reason: Found a cluster with delete failed status: " +
                        failedComputeClusterResponse1.getName(), environmentServiceException.getMessage());
        verify(endpoint, times(2)).list(envCrn);
        verify(endpoint, times(1)).delete(defaultCluster);
        verify(endpoint, times(1)).delete(anotherCluster);
    }

}
