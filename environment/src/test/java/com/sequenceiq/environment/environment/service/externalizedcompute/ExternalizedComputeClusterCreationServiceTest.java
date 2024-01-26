package com.sequenceiq.environment.environment.service.externalizedcompute;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.environment.dto.ExternalizedClusterCreateDto;
import com.sequenceiq.externalizedcompute.api.endpoint.ExternalizedComputeClusterEndpoint;

@ExtendWith(MockitoExtension.class)
class ExternalizedComputeClusterCreationServiceTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:dummyUser:environment:randomGeneratedResource";

    @Mock
    private ExternalizedComputeClusterEndpoint endpoint;

    @InjectMocks
    private ExternalizedComputeClusterCreationService underTest;

    @Test
    void createExternalizedComputeClusterWhenEnvironmentRequestContainsRequestCallToExtShouldHappen() {
        ExternalizedClusterCreateDto dto = ExternalizedClusterCreateDto.builder().withEnvName("test-external").build();
        underTest.createExternalizedComputeCluster(ENVIRONMENT_CRN, dto);
        verify(endpoint, times(1)).create(any());
    }

    @Test
    void createExternalizedComputeClusterWhenExtComputeThrowErrorShouldNotThrowException() {
        when(endpoint.create(any())).thenThrow(new NotFoundException("HTTP 404 Not Found localhost:1111/faultyurl"));
        ExternalizedClusterCreateDto dto = ExternalizedClusterCreateDto.builder().withEnvName("test-external").build();
        assertDoesNotThrow(() -> underTest.createExternalizedComputeCluster(ENVIRONMENT_CRN, dto));
        verify(endpoint, times(1)).create(any());
    }

    @Test
    void createExternalizedComputeClusterWhenEnvironmentRequestDoesNotContainsRequest() {
        underTest.createExternalizedComputeCluster(ENVIRONMENT_CRN, null);
        verify(endpoint, times(0)).create(any());
    }
}