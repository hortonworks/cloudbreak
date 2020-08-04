package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.common.api.type.InstanceGroupType.CORE;
import static com.sequenceiq.common.api.type.InstanceGroupType.GATEWAY;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.LIGHT_DUTY;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.MEDIUM_DUTY_HA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.configuration.CDPConfigService;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.datalake.service.validation.cloudstorage.CloudStorageLocationValidator;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.sdx.api.model.SdxAwsRequest;
import com.sequenceiq.sdx.api.model.SdxAwsSpotParameters;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@ExtendWith(MockitoExtension.class)
@DisplayName("SDX service tests")
class SdxServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:perdos@hortonworks.com";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:default:environment:e438a2db-d650-4132-ae62-242c5ba2f784";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:default:datalake:e438a2db-d650-4132-ae62-242c5ba2f784";

    private static final Long SDX_ID = 2L;

    private static final String CLUSTER_NAME = "test-sdx-cluster";

    @Mock
    private SdxExternalDatabaseConfigurer externalDatabaseConfigurer;

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private StackRequestManifester stackRequestManifester;

    @Mock
    private Clock clock;

    @Mock
    private CloudStorageManifester cloudStorageManifester;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private DistroXV1Endpoint distroXV1Endpoint;

    @Mock
    private SdxNotificationService notificationService;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private DistroxService distroxService;

    @Mock
    private CloudStorageLocationValidator cloudStorageLocationValidator;

    @Mock
    private CDPConfigService cdpConfigService;

    @Mock
    private FlowCancelService flowCancelService;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private TransactionService transactionService;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private SdxService underTest;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testGetSdxClusterWhenClusterNameProvidedShouldReturnSdxClusterWithTheSameNameAsTheRequest() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(CLUSTER_NAME);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(eq("hortonworks"), eq(CLUSTER_NAME)))
                .thenReturn(Optional.of(sdxCluser));
        SdxCluster returnedSdxCluster = underTest.getByNameInAccount(USER_CRN, CLUSTER_NAME);
        assertEquals(sdxCluser, returnedSdxCluster);
    }

    @Test
    void testGetSdxClusterWhenClusterCrnProvidedShouldReturnSdxClusterWithTheSameCrnAsTheRequest() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(CLUSTER_NAME);
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(eq("hortonworks"), eq(ENVIRONMENT_CRN))).thenReturn(Optional.of(sdxCluser));
        SdxCluster returnedSdxCluster = underTest.getByCrn(USER_CRN, ENVIRONMENT_CRN);
        assertEquals(sdxCluser, returnedSdxCluster);
    }

    @Test
    void testGetSdxClusterByAccountIdWhenNoDeployedClusterShouldThrowSdxNotFoundException() {
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.empty());
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> underTest.getByNameInAccount(USER_CRN, "sdxcluster"));
        assertEquals("SDX cluster 'sdxcluster' not found.", notFoundException.getMessage());
    }

    @Test
    void testCreateSdxClusterWhenClusterWithSpecifiedNameAlreadyExistShouldThrowClusterAlreadyExistBadRequestException() {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxClusterRequest.setEnvironment("envir");
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.addTags(tags);
        SdxCluster existing = new SdxCluster();
        existing.setEnvName("envir");
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Collections.singletonList(existing));
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        assertEquals("SDX cluster exists for environment name: envir", badRequestException.getMessage());
    }

    @Test
    void testCreateNOTInternalSdxClusterFromLightDutyTemplateShouldTriggerSdxCreationFlow() throws IOException, TransactionExecutionException {
        when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/runtime/7.1.0/aws/light_duty.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setClusterShape(LIGHT_DUTY);
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.addTags(tags);
        sdxClusterRequest.setEnvironment("envir");
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(anyString(), anyString())).thenReturn(new ArrayList<>());
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        when(clock.getCurrentTimeMillis()).thenReturn(1L);
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AZURE);
        Pair<SdxCluster, FlowIdentifier> result = underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null);
        SdxCluster createdSdxCluster = result.getLeft();
        Assertions.assertEquals(id, createdSdxCluster.getId());
        final ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster capturedSdx = captor.getValue();
        assertEquals("tagecske", capturedSdx.getTags().getValue("mytag"));
        assertEquals(CLUSTER_NAME, capturedSdx.getClusterName());
        assertEquals(LIGHT_DUTY, capturedSdx.getClusterShape());
        assertEquals("envir", capturedSdx.getEnvName());
        assertEquals("hortonworks", capturedSdx.getAccountId());
        assertEquals(USER_CRN, capturedSdx.getInitiatorUserCrn());
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(DatalakeStatusEnum.REQUESTED, "Datalake requested", createdSdxCluster);

        assertEquals(1L, capturedSdx.getCreated());
        assertFalse(capturedSdx.isCreateDatabase());
        assertTrue(createdSdxCluster.getCrn().matches("crn:cdp:datalake:us-west-1:hortonworks:datalake:.*"));
        StackV4Request stackV4Request = JsonUtil.readValue(capturedSdx.getStackRequest(), StackV4Request.class);

        assertEquals(2L, stackV4Request.getInstanceGroups().size());

        InstanceGroupV4Request core = getGroup(stackV4Request, CORE);
        assertEquals(1L, core.getSecurityGroup().getSecurityRules().size());
        assertEquals("0.0.0.0/0", core.getSecurityGroup().getSecurityRules().get(0).getSubnet());
        assertEquals("22", core.getSecurityGroup().getSecurityRules().get(0).getPorts().get(0));

        InstanceGroupV4Request gateway = getGroup(stackV4Request, GATEWAY);
        assertEquals(2L, gateway.getSecurityGroup().getSecurityRules().size());
        assertEquals("0.0.0.0/0", gateway.getSecurityGroup().getSecurityRules().get(0).getSubnet());
        assertEquals("443", gateway.getSecurityGroup().getSecurityRules().get(0).getPorts().get(0));
        assertEquals("0.0.0.0/0", gateway.getSecurityGroup().getSecurityRules().get(1).getSubnet());
        assertEquals("22", gateway.getSecurityGroup().getSecurityRules().get(1).getPorts().get(0));

        verify(sdxReactorFlowManager).triggerSdxCreation(createdSdxCluster);
    }

    @Test
    void testCreateNOTInternalSdxClusterFromLightDutyTemplateWhenLocationSpecifiedWithSlashShouldCreateAndSettedUpBaseLocationWithOUTSlash()
            throws IOException, TransactionExecutionException {
        when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/runtime/7.1.0/aws/light_duty.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setClusterShape(LIGHT_DUTY);
        sdxClusterRequest.setEnvironment("envir");
        SdxCloudStorageRequest cloudStorage = new SdxCloudStorageRequest();
        cloudStorage.setFileSystemType(FileSystemType.S3);
        cloudStorage.setBaseLocation("s3a://some/dir/");
        cloudStorage.setS3(new S3CloudStorageV1Parameters());
        sdxClusterRequest.setCloudStorage(cloudStorage);
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS);
        Pair<SdxCluster, FlowIdentifier> result = underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null);
        SdxCluster createdSdxCluster = result.getLeft();
        assertEquals("s3a://some/dir", createdSdxCluster.getCloudStorageBaseLocation());
    }

    @Test
    void testCreateSdxClusterWithSpotStackRequestContainsRequiredAttributes() throws IOException, TransactionExecutionException {
        when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/runtime/7.1.0/aws/light_duty.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setClusterShape(LIGHT_DUTY);
        sdxClusterRequest.setEnvironment("envir");
        setSpot(sdxClusterRequest);
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS);
        Pair<SdxCluster, FlowIdentifier> result = underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null);
        SdxCluster createdSdxCluster = result.getLeft();
        // AWS 7.1.0 light duty contains exactly 2 instance groups
        assertThat(createdSdxCluster.getStackRequest()).containsSubsequence(
                "{\"aws\":{\"spot\":{\"percentage\":100,\"maxPrice\":0.9}}",
                "{\"aws\":{\"spot\":{\"percentage\":100,\"maxPrice\":0.9}}");
    }

    @Test
    void testCreateNOTInternalSdxClusterFromLightDutyTemplateWhenBaseLocationSpecifiedShouldCreateStackRequestWithSettedUpBaseLocation()
            throws IOException, TransactionExecutionException {
        when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/runtime/7.1.0/aws/light_duty.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        //doNothing().when(cloudStorageLocationValidator.validate("s3a://some/dir", ));
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setClusterShape(LIGHT_DUTY);
        sdxClusterRequest.setEnvironment("envir");
        SdxCloudStorageRequest cloudStorage = new SdxCloudStorageRequest();
        cloudStorage.setFileSystemType(FileSystemType.S3);
        cloudStorage.setBaseLocation("s3a://some/dir");
        cloudStorage.setS3(new S3CloudStorageV1Parameters());
        sdxClusterRequest.setCloudStorage(cloudStorage);
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS);
        Pair<SdxCluster, FlowIdentifier> result = underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null);
        SdxCluster createdSdxCluster = result.getLeft();
        assertEquals("s3a://some/dir", createdSdxCluster.getCloudStorageBaseLocation());
    }

    static Object[][] startParamProvider() {
        return new Object[][]{
                {EnvironmentStatus.ENV_STOPPED, "The environment is stopped. Please start the environment first!"},
                {EnvironmentStatus.STOP_FREEIPA_STARTED, "The environment is stopped. Please start the environment first!"},
                {EnvironmentStatus.START_FREEIPA_STARTED, "The environment is starting. Please wait until finished!"}
        };
    }

    @ParameterizedTest
    @MethodSource("startParamProvider")
    void testCreateButEnvInStoppedStatus(EnvironmentStatus environmentStatus, String exceptionMessage) {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxClusterRequest.setEnvironment("envir");
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.addTags(tags);

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(CloudPlatform.AWS.name());
        detailedEnvironmentResponse.setCrn(Crn.builder()
                .setService(Crn.Service.ENVIRONMENTS)
                .setResourceType(Crn.ResourceType.ENVIRONMENT)
                .setResource(UUID.randomUUID().toString())
                .setAccountId(UUID.randomUUID().toString())
                .build().toString());
        detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null), "BadRequestException should thrown");
        assertEquals(exceptionMessage, badRequestException.getMessage());
    }

    static Object[][] deleteInProgressParamProvider() {
        return new Object[][]{
                {EnvironmentStatus.DELETE_INITIATED},
                {EnvironmentStatus.NETWORK_DELETE_IN_PROGRESS},
                {EnvironmentStatus.RDBMS_DELETE_IN_PROGRESS},
                {EnvironmentStatus.FREEIPA_DELETE_IN_PROGRESS},
                {EnvironmentStatus.CLUSTER_DEFINITION_CLEANUP_PROGRESS},
                {EnvironmentStatus.UMS_RESOURCE_DELETE_IN_PROGRESS},
                {EnvironmentStatus.IDBROKER_MAPPINGS_DELETE_IN_PROGRESS},
                {EnvironmentStatus.S3GUARD_TABLE_DELETE_IN_PROGRESS},
                {EnvironmentStatus.DATAHUB_CLUSTERS_DELETE_IN_PROGRESS},
                {EnvironmentStatus.DATALAKE_CLUSTERS_DELETE_IN_PROGRESS},
                {EnvironmentStatus.PUBLICKEY_DELETE_IN_PROGRESS}
        };
    }

    @ParameterizedTest
    @MethodSource("deleteInProgressParamProvider")
    void testCreateButEnvInDeleteInProgressPhase(EnvironmentStatus environmentStatus) {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxClusterRequest.setEnvironment("envir");
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tag");
        sdxClusterRequest.addTags(tags);

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(CloudPlatform.AWS.name());
        detailedEnvironmentResponse.setCrn(Crn.builder()
                .setService(Crn.Service.ENVIRONMENTS)
                .setResourceType(Crn.ResourceType.ENVIRONMENT)
                .setResource(UUID.randomUUID().toString())
                .setAccountId(UUID.randomUUID().toString())
                .build().toString());
        detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null), "BadRequestException should thrown");
        assertEquals("The environment is in delete in progress phase. Please create a new environment first!", badRequestException.getMessage());
    }

    @Test
    void testListSdxClustersWhenEnvironmentNameProvidedAndTwoSdxIsInTheDatabaseShouldListAllSdxClusterWhichIsTwo() {
        List<SdxCluster> sdxClusters = List.of(new SdxCluster(), new SdxCluster());
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(eq("hortonworks"), eq("envir"))).thenReturn(sdxClusters);
        List<SdxCluster> sdxList = underTest.listSdx(USER_CRN, "envir");
        assertEquals(2, sdxList.size());
    }

    @Test
    void testListSdxClustersWhenEnvironmentCrnProvidedAndTwoSdxIsInTheDatabaseShouldListAllSdxClusterWhichIsTwo() {
        List<SdxCluster> sdxClusters = List.of(new SdxCluster(), new SdxCluster());
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNull(eq("hortonworks"), eq(ENVIRONMENT_CRN))).thenReturn(sdxClusters);
        List<SdxCluster> sdxList = underTest.listSdxByEnvCrn(USER_CRN, ENVIRONMENT_CRN);
        assertEquals(2, sdxList.size());
    }

    @Test
    void testListSdxClustersWhenInvalidEnvironmentNameProvidedShouldThrowBadRequest() {
        String crn = "crsdfadsfdsf sadasf3-df81ae585e10";
        assertThrows(BadRequestException.class, () -> underTest.listSdx(crn, "envir"));
    }

    @Test
    void testDeleteSdxWhennNameIsProvidedAndClusterDoesNotExistShouldThrowNotFoundException() {
        Assertions.assertThrows(com.sequenceiq.cloudbreak.exception.NotFoundException.class,
                () -> underTest.deleteSdx(USER_CRN, CLUSTER_NAME, false));
        verify(sdxClusterRepository, times(1))
                .findByAccountIdAndClusterNameAndDeletedIsNull(eq("hortonworks"), eq(CLUSTER_NAME));
    }

    @Test
    void testDeleteSdxWhenNameIsProvidedShouldInitiateSdxDeletionFlow() {
        SdxCluster sdxCluster = getSdxClusterForDeletionTest();
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxReactorFlowManager.triggerSdxDeletion(any(SdxCluster.class), anyBoolean())).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        mockCBCallForDistroXClusters(Sets.newHashSet());
        underTest.deleteSdx(USER_CRN, "sdx-cluster-name", true);
        verify(sdxReactorFlowManager, times(1)).triggerSdxDeletion(sdxCluster, true);
        final ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.DELETE_REQUESTED, "Datalake deletion requested", sdxCluster);
    }

    @Test
    void testDeleteSdxWhenSdxHasAttachedDataHubsShouldThrowBadRequestBecauseSdxCanNotDeletedIfAttachedClustersAreAvailable() {
        SdxCluster sdxCluster = getSdxClusterForDeletionTest();
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));

        StackViewV4Response stackViewV4Response = new StackViewV4Response();
        stackViewV4Response.setName("existingDistroXCluster");
        mockCBCallForDistroXClusters(Sets.newHashSet(stackViewV4Response));

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.deleteSdx(USER_CRN, "sdx-cluster-name", false));
        assertEquals("The following Data Hub cluster(s) must be terminated before SDX deletion [existingDistroXCluster]", badRequestException.getMessage());
    }

    @Test
    void testDeleteSdxWhenSdxHasExternalDatabaseButCrnIsMissingShouldThrowNotFoundException() {
        SdxCluster sdxCluster = getSdxClusterForDeletionTest();
        sdxCluster.setDatabaseAvailabilityType(SdxDatabaseAvailabilityType.HA);
        sdxCluster.setDatabaseCrn(null);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.deleteSdx(USER_CRN, "sdx-cluster-name", false));
        assertEquals(String.format("Can not find external database for Data Lake: %s", sdxCluster.getClusterName()), badRequestException.getMessage());
    }

    @Test
    void testSyncSdxClusterWhenClusterNameSpecifiedShouldCallStackEndpointExactlyOnce() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(CLUSTER_NAME);
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(eq("hortonworks"), eq(DATALAKE_CRN)))
                .thenReturn(Optional.of(sdxCluser));

        underTest.syncByCrn(USER_CRN, DATALAKE_CRN);

        verify(stackV4Endpoint, times(1)).sync(eq(0L), eq(CLUSTER_NAME), anyString());
    }

    @Test
    void testSdxCreateRazEnabled() throws IOException, TransactionExecutionException {
        when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/runtime/7.1.0/aws/light_duty.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        when(sdxReactorFlowManager.triggerSdxCreation(any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setRuntime("7.2.1");
        sdxClusterRequest.setClusterShape(LIGHT_DUTY);
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.addTags(tags);
        sdxClusterRequest.setEnvironment("envir");
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(anyString(), anyString())).thenReturn(new ArrayList<>());
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        when(clock.getCurrentTimeMillis()).thenReturn(1L);
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AZURE);
        sdxClusterRequest.setEnableRangerRaz(true);
        when(entitlementService.razEnabled(anyString(), anyString())).thenReturn(true);
        Pair<SdxCluster, FlowIdentifier> result = underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null);
        SdxCluster createdSdxCluster = result.getLeft();
        Assertions.assertEquals(id, createdSdxCluster.getId());
        final ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster capturedSdx = captor.getValue();
        assertTrue(capturedSdx.isRangerRazEnabled());
    }

    @Test
    void testSdxCreateRazEnabledNoEntitlement() throws IOException {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setRuntime("7.2.1");
        sdxClusterRequest.setClusterShape(LIGHT_DUTY);
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.addTags(tags);
        sdxClusterRequest.setEnvironment("envir");
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(anyString(), anyString())).thenReturn(new ArrayList<>());
        long id = 10L;
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AZURE);
        sdxClusterRequest.setEnableRangerRaz(true);
        when(entitlementService.razEnabled(anyString(), anyString())).thenReturn(false);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        assertEquals("1. Provisioning Ranger Raz is not enabled for this account.", badRequestException.getMessage());
    }

    @Test
    void testSdxCreateRazEnabledNotAzure() throws IOException {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setRuntime("7.2.1");
        sdxClusterRequest.setClusterShape(LIGHT_DUTY);
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.addTags(tags);
        sdxClusterRequest.setEnvironment("envir");
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(anyString(), anyString())).thenReturn(new ArrayList<>());
        long id = 10L;
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS);
        sdxClusterRequest.setEnableRangerRaz(true);
        when(entitlementService.razEnabled(anyString(), anyString())).thenReturn(true);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        assertEquals("1. Provisioning Ranger Raz is only valid for Microsoft Azure.", badRequestException.getMessage());
    }

    @Test
    void testSdxCreateRazEnabledNoEntitlmentAndNotAzure() throws IOException {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setRuntime("7.2.1");
        sdxClusterRequest.setClusterShape(LIGHT_DUTY);
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.addTags(tags);
        sdxClusterRequest.setEnvironment("envir");
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(anyString(), anyString())).thenReturn(new ArrayList<>());
        long id = 10L;
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS);
        sdxClusterRequest.setEnableRangerRaz(true);
        when(entitlementService.razEnabled(anyString(), anyString())).thenReturn(false);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        assertEquals("1. Provisioning Ranger Raz is not enabled for this account.\n" +
                "2. Provisioning Ranger Raz is only valid for Microsoft Azure.", badRequestException.getMessage());
    }

    @Test
    void testSdxCreateRazEnabled710Runtime() {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setRuntime("7.1.0");
        sdxClusterRequest.setClusterShape(LIGHT_DUTY);
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.addTags(tags);
        sdxClusterRequest.setEnvironment("envir");
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(anyString(), anyString())).thenReturn(new ArrayList<>());
        long id = 10L;
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AZURE);
        sdxClusterRequest.setEnableRangerRaz(true);
        when(entitlementService.razEnabled(anyString(), anyString())).thenReturn(true);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        assertEquals("1. Provisioning Ranger Raz is only valid for CM version >= 7.2.1 and not 7.1.0", badRequestException.getMessage());
    }

    @Test
    void testSdxCreateRazEnabled720Runtime() {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setRuntime("7.2.0");
        sdxClusterRequest.setClusterShape(LIGHT_DUTY);
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.addTags(tags);
        sdxClusterRequest.setEnvironment("envir");
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(anyString(), anyString())).thenReturn(new ArrayList<>());
        long id = 10L;
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AZURE);
        sdxClusterRequest.setEnableRangerRaz(true);
        when(entitlementService.razEnabled(anyString(), anyString())).thenReturn(true);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        assertEquals("1. Provisioning Ranger Raz is only valid for CM version >= 7.2.1 and not 7.2.0", badRequestException.getMessage());
    }

    @Test
    void testSdxCreateMediumDutySdxEnabled() throws IOException, TransactionExecutionException {
        final String runtime = "7.2.2";
        when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/runtime/" + runtime + "/aws/medium_duty_ha.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        when(sdxReactorFlowManager.triggerSdxCreation(any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setRuntime(runtime);
        sdxClusterRequest.setClusterShape(MEDIUM_DUTY_HA);
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.addTags(tags);
        sdxClusterRequest.setEnvironment("envir");
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(anyString(), anyString())).thenReturn(new ArrayList<>());
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        when(clock.getCurrentTimeMillis()).thenReturn(1L);
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS);
        when(entitlementService.mediumDutySdxEnabled(anyString(), anyString())).thenReturn(true);
        Pair<SdxCluster, FlowIdentifier> result = underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null);
        SdxCluster createdSdxCluster = result.getLeft();
        Assertions.assertEquals(id, createdSdxCluster.getId());
        final ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster capturedSdx = captor.getValue();
        assertTrue(capturedSdx.getClusterShape().equals(MEDIUM_DUTY_HA));
    }

    @Test
    void testSdxCreateMediumDutySdxNoEntitlement() throws IOException {
        final String runtime = "7.2.2";
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setRuntime(runtime);
        sdxClusterRequest.setClusterShape(MEDIUM_DUTY_HA);
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.addTags(tags);
        sdxClusterRequest.setEnvironment("envir");
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(anyString(), anyString())).thenReturn(new ArrayList<>());
        long id = 10L;
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS);
        when(entitlementService.mediumDutySdxEnabled(anyString(), anyString())).thenReturn(false);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        assertEquals("1. Provisioning a medium duty data lake cluster is not enabled for this account. " +
                "Contact Cloudera support to enable CDP_MEDIUM_DUTY_SDX entitlement for the account.", badRequestException.getMessage());
    }

    @Test
    void testSdxCreateMediumDutySdxEnabled710Runtime() {
        final String invalidRuntime = "7.1.0";
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setRuntime(invalidRuntime);
        sdxClusterRequest.setClusterShape(MEDIUM_DUTY_HA);
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.addTags(tags);
        sdxClusterRequest.setEnvironment("envir");
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(anyString(), anyString())).thenReturn(new ArrayList<>());
        long id = 10L;
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS);
        when(entitlementService.mediumDutySdxEnabled(anyString(), anyString())).thenReturn(true);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        assertEquals("1. Provisioning a Medium Duty SDX shape is only valid for CM version >= 7.2.2 and not " + invalidRuntime, badRequestException.getMessage());
    }

    @Test
    void testSdxCreateMediumDutySdxEnabled720Runtime() {
        final String invalidRuntime = "7.2.0";
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setRuntime(invalidRuntime);
        sdxClusterRequest.setClusterShape(MEDIUM_DUTY_HA);
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.addTags(tags);
        sdxClusterRequest.setEnvironment("envir");
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(anyString(), anyString())).thenReturn(new ArrayList<>());
        long id = 10L;
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS);
        when(entitlementService.mediumDutySdxEnabled(anyString(), anyString())).thenReturn(true);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        assertEquals("1. Provisioning a Medium Duty SDX shape is only valid for CM version >= 7.2.2 and not " + invalidRuntime, badRequestException.getMessage());
    }

    private void setSpot(SdxClusterRequest sdxClusterRequest) {
        SdxAwsRequest aws = new SdxAwsRequest();
        SdxAwsSpotParameters spot = new SdxAwsSpotParameters();
        spot.setPercentage(100);
        spot.setMaxPrice(0.9);
        aws.setSpot(spot);
        sdxClusterRequest.setAws(aws);
    }

    private void mockCBCallForDistroXClusters(Set<StackViewV4Response> stackViews) {
        when(distroxService.getAttachedDistroXClusters(anyString())).thenReturn(stackViews);
    }

    private void mockEnvironmentCall(SdxClusterRequest sdxClusterRequest, CloudPlatform cloudPlatform) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(cloudPlatform.name());
        detailedEnvironmentResponse.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        detailedEnvironmentResponse.setCrn(Crn.builder()
                .setService(Crn.Service.ENVIRONMENTS)
                .setResourceType(Crn.ResourceType.ENVIRONMENT)
                .setResource(UUID.randomUUID().toString())
                .setAccountId(UUID.randomUUID().toString())
                .build().toString());
        detailedEnvironmentResponse.setCreator(detailedEnvironmentResponse.getCrn());
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
    }

    private SdxCluster getSdxClusterForDeletionTest() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(SDX_ID);
        sdxCluster.setInitiatorUserCrn(USER_CRN);
        sdxCluster.setEnvCrn(ENVIRONMENT_CRN);
        sdxCluster.setEnvName("envir");
        sdxCluster.setClusterName("sdx-cluster-name");
        return sdxCluster;
    }

    private InstanceGroupV4Request getGroup(StackV4Request stack, com.sequenceiq.common.api.type.InstanceGroupType type) {
        for (InstanceGroupV4Request instanceGroup : stack.getInstanceGroups()) {
            if (instanceGroup.getType().equals(type)) {
                return instanceGroup;
            }
        }
        return null;
    }

}
