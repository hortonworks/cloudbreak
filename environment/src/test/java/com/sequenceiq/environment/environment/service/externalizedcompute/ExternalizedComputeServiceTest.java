package com.sequenceiq.environment.environment.service.externalizedcompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.DefaultComputeCluster;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.ExternalizedComputeClusterDto;
import com.sequenceiq.environment.environment.flow.creation.handler.computecluster.ComputeClusterCreationRetrievalTask;
import com.sequenceiq.environment.environment.flow.creation.handler.computecluster.ComputeClusterPollerObject;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.validation.EnvironmentFlowValidatorService;
import com.sequenceiq.environment.exception.EnvironmentServiceException;
import com.sequenceiq.environment.exception.ExternalizedComputeOperationFailedException;
import com.sequenceiq.environment.util.PollingConfig;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterApiStatus;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterCredentialValidationResponse;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterInternalRequest;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;

@ExtendWith(MockitoExtension.class)
class ExternalizedComputeServiceTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:dummyUser:environment:randomGeneratedResource";

    private static final String COMPUTE_CLUSTER_NAME = "computeClusterName";

    @Mock
    private ExternalizedComputeClientService externalizedComputeClientService;

    @Mock
    private PollingService<ComputeClusterPollerObject> computeClusterPollingService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EnvironmentFlowValidatorService validatorService;

    @InjectMocks
    private ExternalizedComputeService underTest;

    @Test
    void createComputeClusterWhenEnvironmentRequestContainsRequestCallToExtShouldHappen() {
        when(validatorService.validateCredentialForExternalizedComputeCluster(any())).thenReturn(ValidationResult.builder().build());
        ReflectionTestUtils.setField(underTest, "externalizedComputeEnabled", true);
        Environment environment = new Environment();
        DefaultComputeCluster defaultComputeCluster = new DefaultComputeCluster();
        defaultComputeCluster.setCreate(true);
        environment.setDefaultComputeCluster(defaultComputeCluster);
        underTest.createComputeCluster(environment);
        verify(externalizedComputeClientService, times(1)).createComputeCluster(any());
    }

    @Test
    void createComputeClusterWhenClusterExists() {
        ReflectionTestUtils.setField(underTest, "externalizedComputeEnabled", true);
        Environment environment = new Environment();
        environment.setName("environmentName");
        environment.setResourceCrn("envCrn");
        DefaultComputeCluster defaultComputeCluster = new DefaultComputeCluster();
        defaultComputeCluster.setCreate(true);
        environment.setDefaultComputeCluster(defaultComputeCluster);
        when(externalizedComputeClientService.getComputeCluster(environment.getResourceCrn(), "default-environmentName-compute-cluster"))
                .thenReturn(Optional.of(new ExternalizedComputeClusterResponse()));
        when(validatorService.validateCredentialForExternalizedComputeCluster(any())).thenReturn(ValidationResult.builder().build());
        underTest.createComputeCluster(environment);
        verify(externalizedComputeClientService, times(0)).createComputeCluster(any());
    }

    @Test
    void createComputeClusterWhenExtComputeThrowErrorShouldThrowException() {
        when(validatorService.validateCredentialForExternalizedComputeCluster(any())).thenReturn(ValidationResult.builder().build());
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
        failedComputeClusterResponse1.setStatusReason("liftie cluster delete failed");

        when(externalizedComputeClientService.list(envCrn))
                .thenReturn(List.of(computeClusterResponse1, computeClusterResponse2))
                .thenReturn(List.of(failedComputeClusterResponse1));
        ReflectionTestUtils.setField(underTest, "externalizedComputeEnabled", true);
        PollingConfig pollingConfig = PollingConfig.builder().withSleepTime(0).withTimeout(10).withTimeoutTimeUnit(TimeUnit.SECONDS).build();
        EnvironmentServiceException environmentServiceException = assertThrows(EnvironmentServiceException.class,
                () -> underTest.deleteComputeCluster(envCrn, pollingConfig, false));

        assertEquals("Compute clusters deletion failed. Reason: Found a compute cluster with delete failed status: " +
                failedComputeClusterResponse1.getName() + ". Failure reason: liftie cluster delete failed", environmentServiceException.getMessage());
        verify(externalizedComputeClientService, times(2)).list(envCrn);
        verify(externalizedComputeClientService, times(1)).deleteComputeCluster(envCrn, defaultCluster, false);
        verify(externalizedComputeClientService, times(1)).deleteComputeCluster(envCrn, anotherCluster, false);
    }

    @Test
    public void awaitComputeClusterCreationTest() {
        when(computeClusterPollingService.pollWithTimeout(
                any(ComputeClusterCreationRetrievalTask.class),
                any(ComputeClusterPollerObject.class),
                anyLong(),
                anyInt(),
                anyInt()))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());

        Environment environment = new Environment();
        environment.setId(1L);
        environment.setResourceCrn("crn");
        underTest.awaitComputeClusterCreation(environment, "computecluster");

        verify(computeClusterPollingService, times(1)).pollWithTimeout(
                any(ComputeClusterCreationRetrievalTask.class),
                any(ComputeClusterPollerObject.class),
                anyLong(),
                anyInt(),
                anyInt());
    }

    @Test
    public void testUpdateDefaultComputeClusterProperties() {
        Environment environment = new Environment();
        environment.setStatus(EnvironmentStatus.AVAILABLE);
        environment.setId(1L);
        environment.setResourceCrn("crn");
        String outboundType = "udr";
        Set<String> workerNodeSubnets = Set.of("subnet1", "subnet2");
        Set<String> kubeApiAuthorizedIpRanges = Set.of("1.1.1.1/16", "2.2.2.2/8");
        ExternalizedComputeClusterDto request = ExternalizedComputeClusterDto.builder()
                .withCreate(true)
                .withPrivateCluster(true)
                .withOutboundType(outboundType)
                .withWorkerNodeSubnetIds(workerNodeSubnets)
                .withKubeApiAuthorizedIpRanges(kubeApiAuthorizedIpRanges)
                .build();

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        when(environmentService.save(environmentArgumentCaptor.capture())).thenReturn(environment);

        underTest.updateDefaultComputeClusterProperties(environment, request);

        verify(environmentService, times(1)).save(any());
        DefaultComputeCluster defaultComputeCluster = environmentArgumentCaptor.getValue().getDefaultComputeCluster();
        assertTrue(defaultComputeCluster.isPrivateCluster());
        assertEquals(outboundType, defaultComputeCluster.getOutboundType());
        assertEquals(workerNodeSubnets, defaultComputeCluster.getWorkerNodeSubnetIds());
        assertEquals(kubeApiAuthorizedIpRanges, defaultComputeCluster.getKubeApiAuthorizedIpRanges());
    }

    @Test
    public void testReInitializeComputeCluster() {
        ReflectionTestUtils.setField(underTest, "externalizedComputeEnabled", true);
        Environment environment = new Environment();
        String environmentName = "environmentName";
        environment.setName(environmentName);
        String envCrn = "envCrn";
        environment.setResourceCrn(envCrn);
        underTest.reInitializeComputeCluster(environment, true);
        ArgumentCaptor<ExternalizedComputeClusterInternalRequest> argumentCaptor = ArgumentCaptor.forClass(
                ExternalizedComputeClusterInternalRequest.class);
        verify(externalizedComputeClientService).reInitializeComputeCluster(argumentCaptor.capture(),
                eq(true));
        ExternalizedComputeClusterInternalRequest request = argumentCaptor.getValue();
        assertEquals(envCrn, request.getEnvironmentCrn());
        assertEquals("default-" + environmentName + "-compute-cluster", request.getName());
        assertTrue(request.isDefaultCluster());
    }

    @Test
    public void testReInitializeComputeClusterButNotEnabled() {
        ReflectionTestUtils.setField(underTest, "externalizedComputeEnabled", false);
        Environment environment = new Environment();
        String environmentName = "environmentName";
        environment.setName(environmentName);
        String envCrn = "envCrn";
        environment.setResourceCrn(envCrn);
        ExternalizedComputeOperationFailedException externalizedComputeOperationFailedException = assertThrows(
                ExternalizedComputeOperationFailedException.class, () -> underTest.reInitializeComputeCluster(environment, true));
        verify(externalizedComputeClientService, times(0)).reInitializeComputeCluster(any(),
                eq(true));
        assertEquals("Externalized compute not enabled", externalizedComputeOperationFailedException.getCause().getMessage());
    }

    @Test
    public void testReInitializeComputeClusterCreationInProgress() {
        ReflectionTestUtils.setField(underTest, "externalizedComputeEnabled", true);
        Environment environment = new Environment();
        String environmentName = "environmentName";
        environment.setName(environmentName);
        String envCrn = "envCrn";
        environment.setResourceCrn(envCrn);
        ExternalizedComputeClusterResponse externalizedComputeClusterResponse = new ExternalizedComputeClusterResponse();
        externalizedComputeClusterResponse.setStatus(ExternalizedComputeClusterApiStatus.CREATE_IN_PROGRESS);
        when(externalizedComputeClientService.getComputeCluster(envCrn, "default-" + environmentName + "-compute-cluster"))
                .thenReturn(Optional.of(externalizedComputeClusterResponse));
        underTest.reInitializeComputeCluster(environment, true);
        verify(externalizedComputeClientService, times(0)).reInitializeComputeCluster(any(),
                eq(true));
    }

    @Test
    public void testCredentialValidation() {
        String region = "region";
        String credential = "credential";
        ExternalizedComputeClusterCredentialValidationResponse validateCredentialResponse = underTest.validateCredential(credential, region);
        verify(externalizedComputeClientService, times(1)).validateCredential(
                credential, region);
    }

}