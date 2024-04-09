package com.sequenceiq.environment.environment.service.externalizedcompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.exception.ExternalizedComputeOperationFailedException;
import com.sequenceiq.externalizedcompute.api.endpoint.ExternalizedComputeClusterEndpoint;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterRequest;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;

@ExtendWith(MockitoExtension.class)
class ExternalizedComputeClientServiceTest {

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String COMPUTE_NAME = "computeName";

    @Mock
    private ExternalizedComputeClusterEndpoint endpoint;

    @Spy
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @InjectMocks
    private ExternalizedComputeClientService underTest;

    @Test
    void testCreateComputeCluster() {
        ExternalizedComputeClusterRequest request = new ExternalizedComputeClusterRequest();
        underTest.createComputeCluster(request);
        verify(endpoint, times(1)).create(eq(request));
    }

    @Test
    void testCreateComputeClusterShouldThrowExceptionWhenCallThrowsWebApplicationException() {
        when(endpoint.create(any())).thenThrow(new BadRequestException("error"));
        ExternalizedComputeClusterRequest request = new ExternalizedComputeClusterRequest();
        ExternalizedComputeOperationFailedException exception = assertThrows(ExternalizedComputeOperationFailedException.class,
                () -> underTest.createComputeCluster(request));
        verify(endpoint, times(1)).create(eq(request));
        assertEquals("Failed to create compute cluster due to: error", exception.getMessage());
    }

    @Test
    void testCreateComputeClusterShouldThrowExceptionWhenCallThrowsOtherException() {
        when(endpoint.create(any())).thenThrow(new RuntimeException("error"));
        ExternalizedComputeClusterRequest request = new ExternalizedComputeClusterRequest();
        ExternalizedComputeOperationFailedException exception = assertThrows(ExternalizedComputeOperationFailedException.class,
                () -> underTest.createComputeCluster(request));
        verify(endpoint, times(1)).create(eq(request));
        assertEquals("Failed to create compute cluster due to: error", exception.getMessage());
    }

    @Test
    void testGetComputeCluster() {
        ExternalizedComputeClusterResponse computeCluster = new ExternalizedComputeClusterResponse();
        computeCluster.setName(COMPUTE_NAME);
        when(endpoint.describe(eq(ENVIRONMENT_CRN), eq(COMPUTE_NAME))).thenReturn(computeCluster);
        Optional<ExternalizedComputeClusterResponse> response = underTest.getComputeCluster(ENVIRONMENT_CRN, COMPUTE_NAME);
        verify(endpoint, times(1)).describe(eq(ENVIRONMENT_CRN), eq(COMPUTE_NAME));
        assertTrue(response.isPresent());
        assertEquals(COMPUTE_NAME, response.get().getName());
    }

    @Test
    void testGetComputeClusterShouldReturnEmptyOptionalWhenClusterNotFound() {
        when(endpoint.describe(eq(ENVIRONMENT_CRN), eq(COMPUTE_NAME))).thenThrow(new NotFoundException());
        Optional<ExternalizedComputeClusterResponse> response = underTest.getComputeCluster(ENVIRONMENT_CRN, COMPUTE_NAME);
        verify(endpoint, times(1)).describe(eq(ENVIRONMENT_CRN), eq(COMPUTE_NAME));
        assertFalse(response.isPresent());
    }
}