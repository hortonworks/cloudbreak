package com.sequenceiq.externalizedcompute.service;

import static com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatusEnum.AVAILABLE;
import static com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatusEnum.CREATE_FAILED;
import static com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatusEnum.CREATE_IN_PROGRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.DeleteClusterResponse;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ListClusterItem;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ValidateCredentialRequest;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ValidateCredentialResponse;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ValidationResult;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.TagResponse;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterCredentialValidationResponse;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterRequest;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatus;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatusEnum;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterFlowManager;
import com.sequenceiq.externalizedcompute.repository.ExternalizedComputeClusterRepository;
import com.sequenceiq.liftie.client.LiftieGrpcClient;

@ExtendWith(MockitoExtension.class)
class ExternalizedComputeClusterServiceTest {

    public static final String ENV_CRN = "envCrn";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    @Spy
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private EnvironmentEndpoint environmentEndpoint;

    @Mock
    private TransactionService transactionService;

    @Mock
    private ExternalizedComputeClusterRepository externalizedComputeClusterRepository;

    @Mock
    private ExternalizedComputeClusterStatusService externalizedComputeClusterStatusService;

    @Mock
    private ExternalizedComputeClusterFlowManager externalizedComputeClusterFlowManager;

    @Mock
    private LiftieGrpcClient liftieGrpcClient;

    @InjectMocks
    private ExternalizedComputeClusterService clusterService;

    @Test
    public void testPrepareComputeClusterCreation() throws TransactionExecutionException {
        ReflectionTestUtils.setField(regionAwareCrnGenerator, "partition", "cdp");
        ReflectionTestUtils.setField(regionAwareCrnGenerator, "region", "us-west-1");
        doAnswer(invocation -> invocation.getArgument(0, Supplier.class).get()).when(transactionService).required(any(Supplier.class));

        ExternalizedComputeClusterRequest externalizedComputeClusterRequest = new ExternalizedComputeClusterRequest();
        externalizedComputeClusterRequest.setName("cluster");
        externalizedComputeClusterRequest.setEnvironmentCrn(ENV_CRN);
        externalizedComputeClusterRequest.setTags(Map.of("key", "value"));

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setTags(new TagResponse());
        detailedEnvironmentResponse.setCrn(ENV_CRN);
        detailedEnvironmentResponse.setCloudPlatform("AWS");
        when(environmentEndpoint.getByCrn(externalizedComputeClusterRequest.getEnvironmentCrn())).thenReturn(detailedEnvironmentResponse);

        Crn userCrn = Crn.fromString(USER_CRN);

        ExternalizedComputeCluster savedCluster = new ExternalizedComputeCluster();
        when(externalizedComputeClusterRepository.save(any())).thenReturn(savedCluster);

        clusterService.prepareComputeClusterCreation(externalizedComputeClusterRequest, true, userCrn);

        ArgumentCaptor<ExternalizedComputeCluster> clusterArgumentCaptor = ArgumentCaptor.forClass(
                ExternalizedComputeCluster.class);
        verify(externalizedComputeClusterRepository).save(clusterArgumentCaptor.capture());

        ExternalizedComputeCluster cluster = clusterArgumentCaptor.getValue();
        assertEquals(externalizedComputeClusterRequest.getName(), cluster.getName());
        assertEquals(externalizedComputeClusterRequest.getEnvironmentCrn(), cluster.getEnvironmentCrn());
        assertEquals("1234", cluster.getAccountId());
        assertTrue(cluster.isDefaultCluster());
        assertNull(cluster.getDeleted());

        verify(externalizedComputeClusterStatusService).setStatus(savedCluster, ExternalizedComputeClusterStatusEnum.CREATE_IN_PROGRESS,
                "Cluster provision initiated");
    }

    @Test
    public void testGetLiftieClusterCrn() {
        ReflectionTestUtils.setField(regionAwareCrnGenerator, "partition", "cdp");
        ReflectionTestUtils.setField(regionAwareCrnGenerator, "region", "us-west-1");

        ExternalizedComputeCluster cluster = new ExternalizedComputeCluster();
        cluster.setName("cluster");
        cluster.setEnvironmentCrn(ENV_CRN);
        cluster.setLiftieName("liftie1");
        cluster.setAccountId("account");

        String liftieClusterCrn = clusterService.getLiftieClusterCrn(cluster);
        assertEquals("crn:cdp:compute:us-west-1:account:cluster:liftie1", liftieClusterCrn);
    }

    @Test
    public void testDeleteLiftieClusterCrn() {
        ReflectionTestUtils.setField(regionAwareCrnGenerator, "partition", "cdp");
        ReflectionTestUtils.setField(regionAwareCrnGenerator, "region", "us-west-1");

        ExternalizedComputeCluster cluster = new ExternalizedComputeCluster();
        cluster.setName("cluster");
        String envCrn = "envCrn";
        cluster.setEnvironmentCrn(envCrn);
        cluster.setLiftieName("liftie1");
        cluster.setAccountId("account");

        when(externalizedComputeClusterRepository.findByIdAndDeletedIsNull(1L)).thenReturn(Optional.of(cluster));
        RegionAwareInternalCrnGenerator internalCrnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        String internalCrn = "internalCrn";
        when(internalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(internalCrn);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(internalCrnGenerator);
        clusterService.initiateDelete(1L, true);

        verify(liftieGrpcClient).deleteCluster("crn:cdp:compute:us-west-1:account:cluster:liftie1", internalCrn, envCrn, true);
    }

    @Test
    public void testDeleteLiftieClusterCrnButDeleteOperationExists() {
        ReflectionTestUtils.setField(regionAwareCrnGenerator, "partition", "cdp");
        ReflectionTestUtils.setField(regionAwareCrnGenerator, "region", "us-west-1");

        ExternalizedComputeCluster cluster = new ExternalizedComputeCluster();
        cluster.setName("cluster");
        String envCrn = "envCrn";
        cluster.setEnvironmentCrn(envCrn);
        cluster.setLiftieName("liftie1");
        cluster.setAccountId("account");

        when(externalizedComputeClusterRepository.findByIdAndDeletedIsNull(1L)).thenReturn(Optional.of(cluster));
        RegionAwareInternalCrnGenerator internalCrnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        String internalCrn = "internalCrn";
        when(internalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(internalCrn);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(internalCrnGenerator);
        String liftieCrn = "crn:cdp:compute:us-west-1:account:cluster:liftie1";
        String existingOperationMessage = "existing operation 'Delete' running";
        when(liftieGrpcClient.deleteCluster(liftieCrn, internalCrn, envCrn, false))
                .thenThrow(new RuntimeException(existingOperationMessage));
        LiftieSharedProto.DescribeClusterResponse describeClusterResponse = mock(LiftieSharedProto.DescribeClusterResponse.class);
        when(describeClusterResponse.getStatus()).thenReturn("RUNNING");
        when(liftieGrpcClient.describeCluster(liftieCrn, envCrn, internalCrn)).thenReturn(describeClusterResponse);
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> clusterService.initiateDelete(1L, false));
        assertEquals(existingOperationMessage, runtimeException.getMessage());

        verify(liftieGrpcClient).deleteCluster(liftieCrn, internalCrn, envCrn, false);
    }

    @Test
    public void testDeleteLiftieClusterCrnButDeleteOperationExistsButDeletionReallyInProgress() {
        ReflectionTestUtils.setField(regionAwareCrnGenerator, "partition", "cdp");
        ReflectionTestUtils.setField(regionAwareCrnGenerator, "region", "us-west-1");

        ExternalizedComputeCluster cluster = new ExternalizedComputeCluster();
        cluster.setName("cluster");
        String envCrn = "envCrn";
        cluster.setEnvironmentCrn(envCrn);
        cluster.setLiftieName("liftie1");
        cluster.setAccountId("account");

        when(externalizedComputeClusterRepository.findByIdAndDeletedIsNull(1L)).thenReturn(Optional.of(cluster));
        RegionAwareInternalCrnGenerator internalCrnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        String internalCrn = "internalCrn";
        when(internalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(internalCrn);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(internalCrnGenerator);
        String liftieCrn = "crn:cdp:compute:us-west-1:account:cluster:liftie1";
        String existingOperationMessage = "existing operation 'Delete' running";
        when(liftieGrpcClient.deleteCluster(liftieCrn, internalCrn, envCrn, false))
                .thenThrow(new RuntimeException(existingOperationMessage));
        LiftieSharedProto.DescribeClusterResponse describeClusterResponse = mock(LiftieSharedProto.DescribeClusterResponse.class);
        when(describeClusterResponse.getStatus()).thenReturn("DELETING");
        when(liftieGrpcClient.describeCluster(liftieCrn, envCrn, internalCrn)).thenReturn(describeClusterResponse);
        clusterService.initiateDelete(1L, false);

        verify(liftieGrpcClient).deleteCluster(liftieCrn, internalCrn, envCrn, false);
    }

    @Test
    public void testReInitializeComputeCluster() {
        ExternalizedComputeClusterRequest externalizedComputeClusterRequest = new ExternalizedComputeClusterRequest();
        externalizedComputeClusterRequest.setName("cluster");
        externalizedComputeClusterRequest.setEnvironmentCrn(ENV_CRN);
        externalizedComputeClusterRequest.setTags(Map.of("key", "value"));

        ExternalizedComputeCluster cluster = new ExternalizedComputeCluster();
        cluster.setName("cluster");
        cluster.setEnvironmentCrn(ENV_CRN);
        cluster.setLiftieName("liftie1");
        cluster.setAccountId("account");

        when(externalizedComputeClusterRepository.findByEnvironmentCrnAndNameAndDeletedIsNull(ENV_CRN, externalizedComputeClusterRequest.getName()))
                .thenReturn(Optional.of(cluster));

        ExternalizedComputeClusterStatus externalizedComputeClusterStatus = new ExternalizedComputeClusterStatus();
        externalizedComputeClusterStatus.setStatus(CREATE_FAILED);
        when(externalizedComputeClusterStatusService.getActualStatus(cluster)).thenReturn(externalizedComputeClusterStatus);

        clusterService.reInitializeComputeCluster(externalizedComputeClusterRequest, false);

        verify(externalizedComputeClusterFlowManager, times(1)).triggerExternalizedComputeClusterReInitialization(cluster);
    }

    @Test
    public void testReInitializeComputeClusterButNotInFailedStateAndNotForce() {
        ExternalizedComputeClusterRequest externalizedComputeClusterRequest = new ExternalizedComputeClusterRequest();
        externalizedComputeClusterRequest.setName("cluster");
        externalizedComputeClusterRequest.setEnvironmentCrn(ENV_CRN);
        externalizedComputeClusterRequest.setTags(Map.of("key", "value"));

        ExternalizedComputeCluster cluster = new ExternalizedComputeCluster();
        cluster.setName("cluster");
        cluster.setEnvironmentCrn(ENV_CRN);
        cluster.setLiftieName("liftie1");
        cluster.setAccountId("account");

        when(externalizedComputeClusterRepository.findByEnvironmentCrnAndNameAndDeletedIsNull(ENV_CRN, externalizedComputeClusterRequest.getName()))
                .thenReturn(Optional.of(cluster));

        ExternalizedComputeClusterStatus externalizedComputeClusterStatus = new ExternalizedComputeClusterStatus();
        externalizedComputeClusterStatus.setStatus(AVAILABLE);
        when(externalizedComputeClusterStatusService.getActualStatus(cluster)).thenReturn(externalizedComputeClusterStatus);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> clusterService.reInitializeComputeCluster(externalizedComputeClusterRequest, false));
        assertEquals("Compute cluster is not in failed state.", badRequestException.getMessage());

        verify(externalizedComputeClusterFlowManager, times(0)).triggerExternalizedComputeClusterReInitialization(cluster);
    }

    @Test
    public void testReInitializeComputeClusterButNotInFailedStateAndForced() {
        ExternalizedComputeClusterRequest externalizedComputeClusterRequest = new ExternalizedComputeClusterRequest();
        externalizedComputeClusterRequest.setName("cluster");
        externalizedComputeClusterRequest.setEnvironmentCrn(ENV_CRN);
        externalizedComputeClusterRequest.setTags(Map.of("key", "value"));

        ExternalizedComputeCluster cluster = new ExternalizedComputeCluster();
        cluster.setName("cluster");
        cluster.setEnvironmentCrn(ENV_CRN);
        cluster.setLiftieName("liftie1");
        cluster.setAccountId("account");

        when(externalizedComputeClusterRepository.findByEnvironmentCrnAndNameAndDeletedIsNull(ENV_CRN, externalizedComputeClusterRequest.getName()))
                .thenReturn(Optional.of(cluster));

        ExternalizedComputeClusterStatus externalizedComputeClusterStatus = new ExternalizedComputeClusterStatus();
        externalizedComputeClusterStatus.setStatus(AVAILABLE);
        when(externalizedComputeClusterStatusService.getActualStatus(cluster)).thenReturn(externalizedComputeClusterStatus);

        clusterService.reInitializeComputeCluster(externalizedComputeClusterRequest, true);

        verify(externalizedComputeClusterFlowManager, times(1)).triggerExternalizedComputeClusterReInitialization(cluster);
    }

    @Test
    public void testReInitializeComputeClusterButInProgress() {
        ExternalizedComputeClusterRequest externalizedComputeClusterRequest = new ExternalizedComputeClusterRequest();
        externalizedComputeClusterRequest.setName("cluster");
        externalizedComputeClusterRequest.setEnvironmentCrn(ENV_CRN);
        externalizedComputeClusterRequest.setTags(Map.of("key", "value"));

        ExternalizedComputeCluster cluster = new ExternalizedComputeCluster();
        cluster.setName("cluster");
        cluster.setEnvironmentCrn(ENV_CRN);
        cluster.setLiftieName("liftie1");
        cluster.setAccountId("account");

        when(externalizedComputeClusterRepository.findByEnvironmentCrnAndNameAndDeletedIsNull(ENV_CRN, externalizedComputeClusterRequest.getName()))
                .thenReturn(Optional.of(cluster));

        ExternalizedComputeClusterStatus externalizedComputeClusterStatus = new ExternalizedComputeClusterStatus();
        externalizedComputeClusterStatus.setStatus(CREATE_IN_PROGRESS);
        when(externalizedComputeClusterStatusService.getActualStatus(cluster)).thenReturn(externalizedComputeClusterStatus);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> clusterService.reInitializeComputeCluster(externalizedComputeClusterRequest, false));
        assertEquals("Compute cluster is under operation.", badRequestException.getMessage());

        verify(externalizedComputeClusterFlowManager, times(0)).triggerExternalizedComputeClusterReInitialization(cluster);
    }

    @Test
    public void testInitiateAuxClusterDeleteSuccess() {
        Long externalizedComputeClusterId = 1L;
        ExternalizedComputeCluster cluster = new ExternalizedComputeCluster();
        cluster.setName("TestCluster");
        cluster.setEnvironmentCrn("envCrn");
        when(externalizedComputeClusterRepository.findByIdAndDeletedIsNull(1L)).thenReturn(Optional.of(cluster));
        ListClusterItem listClusterItem1 = ListClusterItem.newBuilder().setClusterCrn("clusterCrn1").build();
        ListClusterItem listClusterItem2 = ListClusterItem.newBuilder().setClusterCrn("clusterCrn2").build();
        when(liftieGrpcClient.listAuxClusters("envCrn", USER_CRN)).thenReturn(List.of(listClusterItem1, listClusterItem2));
        RegionAwareInternalCrnGenerator internalCrnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        String internalCrn = "internalCrn";
        when(internalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(internalCrn);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(internalCrnGenerator);

        clusterService.initiateAuxClusterDelete(externalizedComputeClusterId, USER_CRN, false);

        ArgumentCaptor<String> crnCaptor = ArgumentCaptor.forClass(String.class);
        verify(liftieGrpcClient).deleteCluster(eq("clusterCrn1"), crnCaptor.capture(), eq("envCrn"), eq(false));
        verify(liftieGrpcClient).deleteCluster(eq("clusterCrn2"), crnCaptor.capture(), eq("envCrn"), eq(false));
        assertEquals("internalCrn", crnCaptor.getValue());
    }

    @Test
    public void testInitiateAuxClusterDeleteFailure() {
        Long externalizedComputeClusterId = 1L;
        ExternalizedComputeCluster cluster = new ExternalizedComputeCluster();
        cluster.setName("TestCluster");
        cluster.setEnvironmentCrn("envCrn");
        when(externalizedComputeClusterRepository.findByIdAndDeletedIsNull(1L)).thenReturn(Optional.of(cluster));
        ListClusterItem listClusterItem1 = ListClusterItem.newBuilder().setClusterCrn("clusterCrn1").build();
        ListClusterItem listClusterItem2 = ListClusterItem.newBuilder().setClusterCrn("clusterCrn2").build();
        when(liftieGrpcClient.listAuxClusters("envCrn", USER_CRN)).thenReturn(List.of(listClusterItem1, listClusterItem2));
        RegionAwareInternalCrnGenerator internalCrnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        String internalCrn = "internalCrn";
        when(internalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(internalCrn);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(internalCrnGenerator);

        when(liftieGrpcClient.deleteCluster("clusterCrn1", internalCrn, ENV_CRN, false)).thenReturn(DeleteClusterResponse.newBuilder().build());
        doThrow(new RuntimeException("liftie error happened")).when(liftieGrpcClient).deleteCluster("clusterCrn2", internalCrn, ENV_CRN, false);

        RuntimeException runtimeException = assertThrows(RuntimeException.class,
                () -> clusterService.initiateAuxClusterDelete(externalizedComputeClusterId, USER_CRN, false));
        assertEquals("Auxiliary compute cluster deletion failed. Cause: " + "liftie error happened", runtimeException.getMessage());
    }

    @Test
    public void testInitiateAuxClusterDeleteAlreadyDeletedOrNotFoundOrDeleteInProgress() {
        Long externalizedComputeClusterId = 1L;
        ExternalizedComputeCluster cluster = new ExternalizedComputeCluster();
        cluster.setName("TestCluster");
        cluster.setEnvironmentCrn("envCrn");
        when(externalizedComputeClusterRepository.findByIdAndDeletedIsNull(1L)).thenReturn(Optional.of(cluster));
        ListClusterItem listClusterItem1 = ListClusterItem.newBuilder().setClusterCrn("clusterCrn1").build();
        ListClusterItem listClusterItem2 = ListClusterItem.newBuilder().setClusterCrn("clusterCrn2").build();
        when(liftieGrpcClient.listAuxClusters("envCrn", USER_CRN)).thenReturn(List.of(listClusterItem1, listClusterItem2));
        RegionAwareInternalCrnGenerator internalCrnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        String internalCrn = "internalCrn";
        when(internalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(internalCrn);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(internalCrnGenerator);

        when(liftieGrpcClient.deleteCluster("clusterCrn1", internalCrn, ENV_CRN, false)).thenReturn(DeleteClusterResponse.newBuilder().build());
        doThrow(new RuntimeException("clusterCrn2 already deleted")).when(liftieGrpcClient).deleteCluster("clusterCrn2", internalCrn, ENV_CRN, false);
        clusterService.initiateAuxClusterDelete(externalizedComputeClusterId, USER_CRN, false);
        doThrow(new RuntimeException("clusterCrn2 not found in database")).when(liftieGrpcClient).deleteCluster("clusterCrn2", internalCrn, ENV_CRN, false);
        clusterService.initiateAuxClusterDelete(externalizedComputeClusterId, USER_CRN, false);
        doThrow(new RuntimeException("existing operation 'Delete'")).when(liftieGrpcClient).deleteCluster("clusterCrn2", internalCrn, ENV_CRN, false);
        clusterService.initiateAuxClusterDelete(externalizedComputeClusterId, USER_CRN, false);
    }

    @Test
    public void testValidateCredential() {
        String actorCrn = "actorCrn";
        String credential = "credential";
        String region = "region";
        when(liftieGrpcClient.validateCredential(any(ValidateCredentialRequest.class), eq(ENV_CRN), eq(actorCrn))).thenReturn(
                ValidateCredentialResponse.newBuilder().setResult("PASSED").build());
        ExternalizedComputeClusterCredentialValidationResponse validateCredentialResult =
                clusterService.validateCredential(ENV_CRN, credential, region, actorCrn);
        ArgumentCaptor<ValidateCredentialRequest> validateCredentialRequestArgumentCaptor = ArgumentCaptor.forClass(
                ValidateCredentialRequest.class);
        verify(liftieGrpcClient, times(1))
                .validateCredential(validateCredentialRequestArgumentCaptor.capture(), eq(ENV_CRN), eq(actorCrn));
        ValidateCredentialRequest capturedRequest = validateCredentialRequestArgumentCaptor.getValue();
        assertEquals(credential, capturedRequest.getCredential());
        assertEquals(region, capturedRequest.getRegion());
        assertTrue(validateCredentialResult.isSuccessful());
    }

    @Test
    public void testValidateCredentialFailed() {
        String actorCrn = "actorCrn";
        String credential = "credential";
        String region = "region";
        when(liftieGrpcClient.validateCredential(any(ValidateCredentialRequest.class), eq(ENV_CRN), eq(actorCrn))).thenReturn(
                ValidateCredentialResponse.newBuilder().setResult("FAILED")
                        .addValidations(ValidationResult.newBuilder().setStatus("FAILED")
                                .setName("IAM Policy Permissions Check")
                                .setDescription("For a given role, the attached policies should have all the necessary permissions " +
                                        "of a default cross account role in AWS ")
                                .setCategory("IAM")
                                .setMessage("IAM Policy validation failed.")
                                .setDetailedMessage("CrossAccount role does not have permissions for these operations : ssm:GetParameter, ssm:GetParameters, " +
                                        "ssm:GetParameterHistory, ssm:GetParametersByPath. Please check if any Service Control Policies are in place. " +
                                        "Documentation:  https://docs.aws.amazon.com/organizations/latest/userguide/orgs_manage_policies_scps.html.").build())
                        .build());
        ExternalizedComputeClusterCredentialValidationResponse validateCredentialResult =
                clusterService.validateCredential(ENV_CRN, credential, region, actorCrn);
        ArgumentCaptor<ValidateCredentialRequest> validateCredentialRequestArgumentCaptor = ArgumentCaptor.forClass(
                ValidateCredentialRequest.class);
        verify(liftieGrpcClient, times(1))
                .validateCredential(validateCredentialRequestArgumentCaptor.capture(), eq(ENV_CRN), eq(actorCrn));
        ValidateCredentialRequest capturedRequest = validateCredentialRequestArgumentCaptor.getValue();
        assertEquals(credential, capturedRequest.getCredential());
        assertEquals(region, capturedRequest.getRegion());
        assertFalse(validateCredentialResult.isSuccessful());
        assertThat(validateCredentialResult.getValidationResults())
                .containsOnly("CrossAccount role does not have permissions for these operations : ssm:GetParameter, ssm:GetParameters, " +
                        "ssm:GetParameterHistory, ssm:GetParametersByPath. Please check if any Service Control Policies are in place. " +
                        "Documentation:  https://docs.aws.amazon.com/organizations/latest/userguide/orgs_manage_policies_scps.html.");
    }

}