package com.sequenceiq.environment.environment.service.externalizedcompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.exception.ExternalizedComputeOperationFailedException;
import com.sequenceiq.externalizedcompute.api.endpoint.ExternalizedComputeClusterInternalEndpoint;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterInternalRequest;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;

@ExtendWith(MockitoExtension.class)
class ExternalizedComputeClientServiceTest {

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String COMPUTE_NAME = "computeName";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private ExternalizedComputeClusterInternalEndpoint endpoint;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Spy
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @InjectMocks
    private ExternalizedComputeClientService underTest;

    @BeforeEach
    void setUp() {
        RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        lenient().when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        lenient().when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("internalCrn");
    }

    @Test
    void testCreateComputeCluster() {
        ExternalizedComputeClusterInternalRequest request = new ExternalizedComputeClusterInternalRequest();
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createComputeCluster(request));
        verify(endpoint, times(1)).create(eq(request), eq(USER_CRN));
    }

    @Test
    void testCreateComputeClusterShouldThrowExceptionWhenCallThrowsWebApplicationException() {
        when(endpoint.create(any(), anyString())).thenThrow(new BadRequestException("error"));
        ExternalizedComputeClusterInternalRequest request = new ExternalizedComputeClusterInternalRequest();
        ExternalizedComputeOperationFailedException exception = assertThrows(ExternalizedComputeOperationFailedException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createComputeCluster(request)));
        verify(endpoint, times(1)).create(eq(request), eq(USER_CRN));
        assertEquals("Failed to create compute cluster due to: error", exception.getMessage());
    }

    @Test
    void testCreateComputeClusterShouldThrowExceptionWhenCallThrowsOtherException() {
        when(endpoint.create(any(), anyString())).thenThrow(new RuntimeException("error"));
        ExternalizedComputeClusterInternalRequest request = new ExternalizedComputeClusterInternalRequest();
        ExternalizedComputeOperationFailedException exception = assertThrows(ExternalizedComputeOperationFailedException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createComputeCluster(request)));
        verify(endpoint, times(1)).create(eq(request), eq(USER_CRN));
        assertEquals("Failed to create compute cluster due to: error", exception.getMessage());
    }

    @Test
    void testGetComputeCluster() {
        ExternalizedComputeClusterResponse computeCluster = new ExternalizedComputeClusterResponse();
        computeCluster.setName(COMPUTE_NAME);
        when(endpoint.describe(eq(ENVIRONMENT_CRN), eq(COMPUTE_NAME))).thenReturn(computeCluster);
        Optional<ExternalizedComputeClusterResponse> response = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getComputeCluster(ENVIRONMENT_CRN, COMPUTE_NAME));
        verify(endpoint, times(1)).describe(eq(ENVIRONMENT_CRN), eq(COMPUTE_NAME));
        assertTrue(response.isPresent());
        assertEquals(COMPUTE_NAME, response.get().getName());
    }

    @Test
    void testGetComputeClusterShouldReturnEmptyOptionalWhenClusterNotFound() {
        when(endpoint.describe(eq(ENVIRONMENT_CRN), eq(COMPUTE_NAME))).thenThrow(new NotFoundException());
        Optional<ExternalizedComputeClusterResponse> response = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getComputeCluster(ENVIRONMENT_CRN, COMPUTE_NAME));
        verify(endpoint, times(1)).describe(eq(ENVIRONMENT_CRN), eq(COMPUTE_NAME));
        assertFalse(response.isPresent());
    }

    @Test
    void deleteCluster() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            underTest.deleteComputeCluster(ENVIRONMENT_CRN, COMPUTE_NAME, true);
        });
        verify(endpoint, times(1)).delete(ENVIRONMENT_CRN, USER_CRN, COMPUTE_NAME, true);
    }

    @Test
    void validateCredential() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            underTest.validateCredential(ENVIRONMENT_CRN, "credential", "region");
        });
        verify(endpoint, times(1)).validateCredential(ENVIRONMENT_CRN, "credential", "region", USER_CRN);
    }
}