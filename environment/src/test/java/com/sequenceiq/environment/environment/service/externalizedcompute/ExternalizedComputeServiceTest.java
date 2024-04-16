package com.sequenceiq.environment.environment.service.externalizedcompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.environment.environment.domain.DefaultComputeCluster;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.exception.EnvironmentServiceException;
import com.sequenceiq.environment.exception.ExternalizedComputeOperationFailedException;
import com.sequenceiq.environment.util.PollingConfig;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterApiStatus;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;

@ExtendWith(MockitoExtension.class)
class ExternalizedComputeServiceTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:dummyUser:environment:randomGeneratedResource";

    private static final String COMPUTE_CLUSTER_NAME = "computeClusterName";

    @Mock
    private ExternalizedComputeClientService externalizedComputeClientService;

    @InjectMocks
    private ExternalizedComputeService underTest;

    @Test
    void createComputeClusterWhenEnvironmentRequestContainsRequestCallToExtShouldHappen() {
        ReflectionTestUtils.setField(underTest, "externalizedComputeEnabled", true);
        Environment environment = new Environment();
        DefaultComputeCluster defaultComputeCluster = new DefaultComputeCluster();
        defaultComputeCluster.setCreate(true);
        environment.setDefaultComputeCluster(defaultComputeCluster);
        underTest.createComputeCluster(environment);
        verify(externalizedComputeClientService, times(1)).createComputeCluster(any());
    }

    @Test
    void createComputeClusterWhenExtComputeThrowErrorShouldThrowException() {
        ReflectionTestUtils.setField(underTest, "externalizedComputeEnabled", true);
        when(externalizedComputeClientService.createComputeCluster(any())).thenThrow(new NotFoundException("HTTP 404 Not Found localhost:1111/faultyurl"));
        Environment environment = new Environment();
        DefaultComputeCluster defaultComputeCluster = new DefaultComputeCluster();
        defaultComputeCluster.setCreate(true);
        environment.setDefaultComputeCluster(defaultComputeCluster);
        ExternalizedComputeOperationFailedException exception = assertThrows(ExternalizedComputeOperationFailedException.class,
                () -> underTest.createComputeCluster(environment));
        verify(externalizedComputeClientService, times(1)).createComputeCluster(any());
        assertEquals("Could not create compute cluster: HTTP 404 Not Found localhost:1111/faultyurl", exception.getMessage());
    }

    @Test
    void createComputeClusterWhenEnvironmentRequestDoesNotContainsRequest() {
        Environment environment = new Environment();
        DefaultComputeCluster defaultComputeCluster = new DefaultComputeCluster();
        defaultComputeCluster.setCreate(false);
        environment.setDefaultComputeCluster(defaultComputeCluster);
        underTest.createComputeCluster(environment);
        verify(externalizedComputeClientService, times(0)).createComputeCluster(any());
    }

    @Test
    void testGetComputeCluster() {
        ExternalizedComputeClusterResponse externalizedComputeCluster = new ExternalizedComputeClusterResponse();
        externalizedComputeCluster.setName(COMPUTE_CLUSTER_NAME);
        externalizedComputeCluster.setEnvironmentCrn(ENVIRONMENT_CRN);
        when(externalizedComputeClientService.getComputeCluster(eq(ENVIRONMENT_CRN), eq(COMPUTE_CLUSTER_NAME)))
                .thenReturn(Optional.of(externalizedComputeCluster));
        Optional<ExternalizedComputeClusterResponse> response = underTest.getComputeCluster(ENVIRONMENT_CRN, COMPUTE_CLUSTER_NAME);
        assertTrue(response.isPresent());
        assertEquals(COMPUTE_CLUSTER_NAME, response.get().getName());
        assertEquals(ENVIRONMENT_CRN, response.get().getEnvironmentCrn());
    }

    @Test
    void testGetDefaultComputeClusterName() {
        String computeClusterName = underTest.getDefaultComputeClusterName("envName");
        assertEquals("default-envName-compute-cluster", computeClusterName);
    }

    @Test
    void createComputeClusterWhenExtComputeThrowErrorShouldThrowExceptionIfExtComputeClusterDisabled() {
        ReflectionTestUtils.setField(underTest, "externalizedComputeEnabled", false);
        Environment environment = new Environment();
        DefaultComputeCluster defaultComputeCluster = new DefaultComputeCluster();
        defaultComputeCluster.setCreate(true);
        environment.setDefaultComputeCluster(defaultComputeCluster);
        ExternalizedComputeOperationFailedException exception = assertThrows(ExternalizedComputeOperationFailedException.class,
                () -> underTest.createComputeCluster(environment));
        verify(externalizedComputeClientService, times(0)).createComputeCluster(any());
        assertEquals("Could not create compute cluster: Externalized compute not enabled", exception.getMessage());
    }

    @Test
    public void deleteComputeClusterTest() {
        String envCrn = "envcrn";
        ExternalizedComputeClusterResponse computeClusterResponse1 = new ExternalizedComputeClusterResponse();
        String defaultCluster = "default-cluster";
        computeClusterResponse1.setName(defaultCluster);
        ExternalizedComputeClusterResponse computeClusterResponse2 = new ExternalizedComputeClusterResponse();
        String anotherCluster = "another-cluster";
        computeClusterResponse2.setName(anotherCluster);
        when(externalizedComputeClientService.list(envCrn))
                .thenReturn(List.of(computeClusterResponse1, computeClusterResponse2))
                .thenReturn(List.of(computeClusterResponse1, computeClusterResponse2))
                .thenReturn(List.of());
        ReflectionTestUtils.setField(underTest, "externalizedComputeEnabled", true);
        PollingConfig pollingConfig = PollingConfig.builder().withSleepTime(0).withTimeout(10).withTimeoutTimeUnit(TimeUnit.SECONDS).build();
        underTest.deleteComputeCluster(envCrn, pollingConfig, true);
        verify(externalizedComputeClientService, times(3)).list(envCrn);
        verify(externalizedComputeClientService, times(1)).deleteComputeCluster(envCrn, defaultCluster, true);
        verify(externalizedComputeClientService, times(1)).deleteComputeCluster(envCrn, anotherCluster, true);
    }

    @Test
    public void deleteComputeClusterTestWithDeleteFailed() {
        ReflectionTestUtils.setField(underTest, "externalizedComputeEnabled", true);
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

        when(externalizedComputeClientService.list(envCrn))
                .thenReturn(List.of(computeClusterResponse1, computeClusterResponse2))
                .thenReturn(List.of(failedComputeClusterResponse1));
        ReflectionTestUtils.setField(underTest, "externalizedComputeEnabled", true);
        PollingConfig pollingConfig = PollingConfig.builder().withSleepTime(0).withTimeout(10).withTimeoutTimeUnit(TimeUnit.SECONDS).build();
        EnvironmentServiceException environmentServiceException = assertThrows(EnvironmentServiceException.class,
                () -> underTest.deleteComputeCluster(envCrn, pollingConfig, false));

        assertEquals("Compute clusters deletion failed. Reason: Found a compute cluster with delete failed status: " +
                failedComputeClusterResponse1.getName(), environmentServiceException.getMessage());
        verify(externalizedComputeClientService, times(2)).list(envCrn);
        verify(externalizedComputeClientService, times(1)).deleteComputeCluster(envCrn, defaultCluster, false);
        verify(externalizedComputeClientService, times(1)).deleteComputeCluster(envCrn, anotherCluster, false);
    }
}