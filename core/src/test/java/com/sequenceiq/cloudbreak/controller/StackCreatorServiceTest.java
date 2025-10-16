package com.sequenceiq.cloudbreak.controller;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_IDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.controller.validation.ParametersValidator;
import com.sequenceiq.cloudbreak.controller.validation.network.MultiAzValidator;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateValidatorAndUpdater;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackV4RequestToStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.service.ClusterCreationSetupService;
import com.sequenceiq.cloudbreak.service.NodeCountLimitValidator;
import com.sequenceiq.cloudbreak.service.StackUnderOperationService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.decorator.StackDecorator;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.java.JavaVersionValidator;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeValidatorService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.validation.SeLinuxValidationService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
class StackCreatorServiceTest {

    private static final String STACK_CRN = "crn:cdp:sdx:us-west-1:1234:sdxcluster:mystack";

    private static final Long WORKSPACE_ID = 1L;

    private static final String INSTANCE_GROUP = "INSTANCE_GROUP";

    private static final String RECIPE_NAME = "RECIPE_NAME";

    private static final String STACK_NAME = "STACK_NAME";

    private static final String AWS_PLATFORM = "AWS";

    private static final String YARN_PLATFORM = "YARN";

    private static final String ACCOUNT_ID = "accountId";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:userName";

    private static final String INTERNAL_USER_CRN = "crn:altus:iam:us-west-1:" + ACCOUNT_ID + ":user:__internal__actor__";

    @Mock
    private StackDecorator stackDecorator;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private ClusterCreationSetupService clusterCreationService;

    @Mock
    private StackService stackService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private ImageService imageService;

    @Mock
    private TemplateValidatorAndUpdater templateValidatorAndUpdater;

    @Mock
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Mock
    private TransactionService transactionService;

    @Mock
    private StackUnderOperationService stackUnderOperationService;

    @Mock
    private ParametersValidator parametersValidator;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private CredentialClientService credentialClientService;

    @Mock
    private ExecutorService executorService;

    @Mock
    private CloudbreakMetricService metricService;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private RecipeService recipeService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @InjectMocks
    private StackCreatorService underTest;

    @InjectMocks
    private StackCreatorService underTestSpy;

    @Mock
    private ValidationResult validationResult;

    @Mock
    private Stack stack;

    @Mock
    private DetailedEnvironmentResponse environmentResponse;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private NodeCountLimitValidator nodeCountLimitValidator;

    @Mock
    private JavaVersionValidator javaVersionValidator;

    @Mock
    private MultiAzValidator multiAzValidator;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Mock
    private RecipeValidatorService recipeValidatorService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private DetailedEnvironmentResponse environment;

    @Mock
    private StackV4RequestToStackConverter stackV4RequestToStackConverter;

    @Mock
    private SeLinuxValidationService seLinuxValidationService;

    static Object[][] fillInstanceMetadataTestWhenSubnetAndAvailabilityZoneAndRackIdAndRoundRobinDataProvider() {
        return new Object[][]{
                // testCaseName subnetId availabilityZone
                {"subnetId=\"subnet-1\", availabilityZone=null", "subnet-1", null},
                {"subnetId=\"subnet-1\", availabilityZone=\"az-1\"", "subnet-1", "az-1"},
        };
    }

    @BeforeEach
    void before() {
        underTestSpy = spy(new StackCreatorService());
        MockitoAnnotations.openMocks(this);
        lenient().when(environmentClientService.getByCrn(any())).thenReturn(environment);
        lenient().when(environment.getRegions()).thenReturn(mock());
    }

    @Test
    void shouldThrowBadRequestWhenStackNameAlreadyExists() {
        User user = new User();
        Workspace workspace = getWorkspace();
        StackV4Request stackRequest = getStackV4Request();
        when(regionAwareCrnGenerator.generateCrnStringWithUuid(any(), anyString())).thenReturn(STACK_CRN);

        lenient().doNothing().when(nodeCountLimitValidator).validateProvision(any(), any());
        when(stackDtoService.getStackViewByNameOrCrnOpt(any(), anyString())).thenReturn(Optional.of(mock(StackView.class)));

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createStack(user, workspace, stackRequest, false)));

        assertEquals("Cluster already exists: STACK_NAME", e.getMessage());
        verify(recipeValidatorService).validateRecipeExistenceOnInstanceGroups(any(), any());
        verify(stackDtoService).getStackViewByNameOrCrnOpt(NameOrCrn.ofName(STACK_NAME), ACCOUNT_ID);
    }

    @Test
    void testArm64ShouldNotBeUsedIfCdhVersionOlderThan731() {
        User user = new User();
        Workspace workspace = getWorkspace();
        StackV4Request stackRequest = getStackV4Request();
        stackRequest.setCluster(new ClusterV4Request());
        stackRequest.getCluster().setBlueprintName("test");
        stackRequest.setArchitecture(Architecture.ARM64.getName());
        when(regionAwareCrnGenerator.generateCrnStringWithUuid(any(), anyString())).thenReturn(STACK_CRN);
        when(stackDtoService.getStackViewByNameOrCrnOpt(any(), anyString())).thenReturn(Optional.empty());
        when(blueprintService.getCdhVersion(any(), any())).thenReturn("7.3.0.");

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createStack(user, workspace, stackRequest, true)));

        assertEquals("The selected architecture (arm64) is not supported in this cdh version (7.3.0.).", e.getMessage());
        verify(recipeValidatorService).validateRecipeExistenceOnInstanceGroups(any(), any());
        verify(stackDtoService).getStackViewByNameOrCrnOpt(NameOrCrn.ofName(STACK_NAME), ACCOUNT_ID);
    }

    @Test
    void throwExceptionIfImageAndInstanceTypeArchitectureIsNotTheSameImageIsX86AndInstanceIsArm64() {
        StackV4Request stackRequest = getStackV4Request();
        stackRequest.setCloudPlatform(CloudPlatform.AWS);
        InstanceTemplateV4Request template = new InstanceTemplateV4Request();
        template.setInstanceType("r7gd.2xlarge");
        stackRequest.getInstanceGroups().getFirst().setTemplate(template);

        StatedImage statedImage = mock(StatedImage.class);
        Image image = mock(Image.class);
        when(statedImage.getImage()).thenReturn(image);
        when(image.getUuid()).thenReturn("a-b-c-1");
        when(image.getArchitecture()).thenReturn("x86_64");

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateImageAndInstanceTypeArchitectureForAws(stackRequest, statedImage)));

        assertEquals("The selected image a-b-c-1 has different architecture x86_64 than the selected r7gd.2xlarge instance type's arm64 architecture.",
                exception.getMessage());
    }

    @Test
    void throwExceptionIfImageAndInstanceTypeArchitectureIsNotTheSameImageIsArm64AndInstanceIsX86() {
        StackV4Request stackRequest = getStackV4Request();
        stackRequest.setCloudPlatform(CloudPlatform.AWS);
        InstanceTemplateV4Request template = new InstanceTemplateV4Request();
        template.setInstanceType("m5.xlarge");
        stackRequest.getInstanceGroups().getFirst().setTemplate(template);

        StatedImage statedImage = mock(StatedImage.class);
        Image image = mock(Image.class);
        when(statedImage.getImage()).thenReturn(image);
        when(image.getUuid()).thenReturn("a-b-c-1");
        when(image.getArchitecture()).thenReturn("arm64");

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateImageAndInstanceTypeArchitectureForAws(stackRequest, statedImage)));

        assertEquals("The selected image a-b-c-1 has different architecture arm64 than the selected m5.xlarge instance type's x86_64 architecture.",
                exception.getMessage());
    }

    @Test
    void doNotThrowExceptionIfImageAndInstanceHasTheSameArchitecture() {
        StackV4Request stackRequest = getStackV4Request();
        stackRequest.setCloudPlatform(CloudPlatform.AWS);
        InstanceTemplateV4Request template = new InstanceTemplateV4Request();
        template.setInstanceType("m5.xlarge");
        stackRequest.getInstanceGroups().getFirst().setTemplate(template);

        StatedImage statedImage = mock(StatedImage.class);
        Image image = mock(Image.class);
        when(statedImage.getImage()).thenReturn(image);
        when(image.getArchitecture()).thenReturn("x86_64");

        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.validateImageAndInstanceTypeArchitectureForAws(stackRequest, statedImage)));
    }

    private TagsV4Request isCodClusterTag(boolean codRequest) {
        TagsV4Request tagsV4Request = new TagsV4Request();
        tagsV4Request.setApplication(Map.of("is_cod_cluster", String.valueOf(codRequest)));
        return tagsV4Request;
    }

    private StackV4Request getStackV4Request() {
        StackV4Request stackRequest = new StackV4Request();
        stackRequest.setName(STACK_NAME);
        InstanceGroupV4Request instanceGroupV4Request = new InstanceGroupV4Request();
        instanceGroupV4Request.setName(INSTANCE_GROUP);
        instanceGroupV4Request.setRecipeNames(Set.of(RECIPE_NAME));
        stackRequest.setInstanceGroups(List.of(instanceGroupV4Request));
        return stackRequest;
    }

    private Workspace getWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        return workspace;
    }

    @Test
    void testShouldUseBaseCMImageShouldReturnFalseWhenCmRequestIsNotPresentAndPlatformIsNotYarn() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();

        boolean actual = underTest.shouldUseBaseCMImage(clusterV4Request, AWS_PLATFORM);

        assertFalse(actual);
    }

    @Test
    void testShouldUseBaseCMImageShouldReturnTrueWhenCmRequestIsNotPresentAndPlatformIsYarn() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();

        boolean actual = underTest.shouldUseBaseCMImage(clusterV4Request, YARN_PLATFORM);

        assertTrue(actual);
    }

    @Test
    void testShouldUseBaseCMImageShouldReturnTrueWithCMImageWithCmRepoAndImageIsNotPresentAndPlatformIsNotYarn() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        ClouderaManagerV4Request cmRequest = new ClouderaManagerV4Request();
        ClouderaManagerRepositoryV4Request cmRepoRequest = new ClouderaManagerRepositoryV4Request();
        cmRequest.setRepository(cmRepoRequest);
        clusterV4Request.setCm(cmRequest);

        boolean base = underTest.shouldUseBaseCMImage(clusterV4Request, AWS_PLATFORM);

        assertTrue(base);
    }

    @Test
    void testShouldUseBaseCMImageShouldReturnTrueWithCMImageWithCmRepoAndImageIsNotPresentAndPlatformIsYarn() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        ClouderaManagerV4Request cmRequest = new ClouderaManagerV4Request();
        ClouderaManagerRepositoryV4Request cmRepoRequest = new ClouderaManagerRepositoryV4Request();
        cmRequest.setRepository(cmRepoRequest);
        clusterV4Request.setCm(cmRequest);

        boolean base = underTest.shouldUseBaseCMImage(clusterV4Request, YARN_PLATFORM);

        assertTrue(base);
    }

    @Test
    void testShouldUseBaseCMImageWithProductsAndPlatformIsNotYarn() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        ClouderaManagerV4Request cmRequest = new ClouderaManagerV4Request();
        ClouderaManagerProductV4Request cdpRequest = new ClouderaManagerProductV4Request();
        cdpRequest.setName("CDP");
        cdpRequest.setParcel("parcel");
        cdpRequest.setVersion("version");
        cdpRequest.setCsd(List.of("csd"));
        cmRequest.setProducts(List.of(cdpRequest));
        clusterV4Request.setCm(cmRequest);

        boolean base = underTest.shouldUseBaseCMImage(clusterV4Request, AWS_PLATFORM);

        assertTrue(base);
    }

    @Test
    void testShouldUseBaseCMImageWithProductsAndPlatformIsYarn() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        ClouderaManagerV4Request cmRequest = new ClouderaManagerV4Request();
        ClouderaManagerProductV4Request cdpRequest = new ClouderaManagerProductV4Request();
        cdpRequest.setName("CDP");
        cdpRequest.setParcel("parcel");
        cdpRequest.setVersion("version");
        cdpRequest.setCsd(List.of("csd"));
        cmRequest.setProducts(List.of(cdpRequest));
        clusterV4Request.setCm(cmRequest);

        boolean base = underTest.shouldUseBaseCMImage(clusterV4Request, YARN_PLATFORM);

        assertTrue(base);
    }

    @Test
    void testShouldUseBaseCMImageWithProductsAndCmRepoAndPlatformIsNotYarn() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        ClouderaManagerV4Request cmRequest = new ClouderaManagerV4Request();
        ClouderaManagerProductV4Request cdpRequest = new ClouderaManagerProductV4Request();
        cdpRequest.setName("CDP");
        cdpRequest.setParcel("parcel");
        cdpRequest.setVersion("version");
        cdpRequest.setCsd(List.of("csd"));
        cmRequest.setProducts(List.of(cdpRequest));
        ClouderaManagerRepositoryV4Request cmRepoRequest = new ClouderaManagerRepositoryV4Request();
        cmRequest.setRepository(cmRepoRequest);
        clusterV4Request.setCm(cmRequest);

        boolean base = underTest.shouldUseBaseCMImage(clusterV4Request, AWS_PLATFORM);

        assertTrue(base);
    }

    @Test
    void testShouldUseBaseCMImageWithProductsAndCmRepoAndPlatformIsYarn() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        ClouderaManagerV4Request cmRequest = new ClouderaManagerV4Request();
        ClouderaManagerProductV4Request cdpRequest = new ClouderaManagerProductV4Request();
        cdpRequest.setName("CDP");
        cdpRequest.setParcel("parcel");
        cdpRequest.setVersion("version");
        cdpRequest.setCsd(List.of("csd"));
        cmRequest.setProducts(List.of(cdpRequest));
        ClouderaManagerRepositoryV4Request cmRepoRequest = new ClouderaManagerRepositoryV4Request();
        cmRequest.setRepository(cmRepoRequest);
        clusterV4Request.setCm(cmRequest);

        boolean base = underTest.shouldUseBaseCMImage(clusterV4Request, YARN_PLATFORM);

        assertTrue(base);
    }

    @Test
    void testFillInstanceMetadataWhenMaster() {
        Stack stack = new Stack();
        InstanceGroup masterGroup = getARequestGroup("master", 1, InstanceGroupType.GATEWAY);
        InstanceGroup workerGroup = getARequestGroup("worker", 2, InstanceGroupType.CORE);
        InstanceGroup computeGroup = getARequestGroup("compute", 4, InstanceGroupType.CORE);
        stack.setInstanceGroups(Set.of(masterGroup, workerGroup, computeGroup));
        underTest.fillInstanceMetadata(environmentResponse, stack);

        Map<String, Set<InstanceMetaData>> hostGroupInstances = stack.getInstanceGroups().stream().collect(
                Collectors.toMap(InstanceGroup::getGroupName, InstanceGroup::getAllInstanceMetaData));
        long privateIdStart = 0L;
        validateInstanceMetadataPrivateId("master", 1, privateIdStart, hostGroupInstances.get("master"));

        privateIdStart = 1L;
        validateInstanceMetadataPrivateId("compute", 4, privateIdStart, hostGroupInstances.get("compute"));

        privateIdStart = 5L;
        validateInstanceMetadataPrivateId("worker", 2, privateIdStart, hostGroupInstances.get("worker"));
    }

    @Test
    void testFillInstanceMetadataWhenManager() {
        Stack stack = new Stack();
        InstanceGroup managerGroup = getARequestGroup("manager", 1, InstanceGroupType.GATEWAY);
        InstanceGroup gatewayGroup = getARequestGroup("gateway", 2, InstanceGroupType.CORE);
        InstanceGroup computeGroup = getARequestGroup("compute", 0, InstanceGroupType.CORE);
        InstanceGroup workerGroup = getARequestGroup("worker", 3, InstanceGroupType.CORE);
        InstanceGroup masterGroup = getARequestGroup("master", 2, InstanceGroupType.CORE);
        stack.setInstanceGroups(Set.of(masterGroup, workerGroup, computeGroup, managerGroup, gatewayGroup));

        underTest.fillInstanceMetadata(environmentResponse, stack);

        Map<String, Set<InstanceMetaData>> hostGroupInstances = stack.getInstanceGroups().stream().collect(
                Collectors.toMap(InstanceGroup::getGroupName, InstanceGroup::getAllInstanceMetaData));

        long privateIdStart = 0L;
        validateInstanceMetadataPrivateId("manager", 1, privateIdStart, hostGroupInstances.get("manager"));

        privateIdStart = 1L;
        validateInstanceMetadataPrivateId("compute", 0, privateIdStart, hostGroupInstances.get("compute"));

        privateIdStart = 1L;
        validateInstanceMetadataPrivateId("gateway", 2, privateIdStart, hostGroupInstances.get("gateway"));

        privateIdStart = 3L;
        validateInstanceMetadataPrivateId("master", 2, privateIdStart, hostGroupInstances.get("master"));

        privateIdStart = 5L;
        validateInstanceMetadataPrivateId("worker", 3, privateIdStart, hostGroupInstances.get("worker"));
    }

    @Test
    void fillInstanceMetadataTestWhenSubnetAndAvZAndRackIdAndRoundRobinForDH() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        InstanceGroupNetwork network = new InstanceGroupNetwork();
        network.setCloudPlatform("aws");
        network.setAttributes(Json.silent(Map.of(SUBNET_IDS, List.of("SUB_1", "SUB_2", "SUB_3"))));
        InstanceGroup masterGroup = getARequestGroup("master", 2, InstanceGroupType.CORE);
        masterGroup.setInstanceGroupNetwork(network);
        InstanceGroup gatewayGroup = getARequestGroup("gateway", 2, InstanceGroupType.GATEWAY);
        gatewayGroup.setInstanceGroupNetwork(network);
        InstanceGroup coreGroup = getARequestGroup("core", 3, InstanceGroupType.CORE);
        coreGroup.setInstanceGroupNetwork(network);
        InstanceGroup auxiliaryGroup = getARequestGroup("auxiliary", 1, InstanceGroupType.CORE);
        auxiliaryGroup.setInstanceGroupNetwork(network);
        InstanceGroup idbrokerGroup = getARequestGroup("idbroker", 2, InstanceGroupType.CORE);
        idbrokerGroup.setInstanceGroupNetwork(network);
        stack.setInstanceGroups(Set.of(masterGroup, coreGroup, auxiliaryGroup, idbrokerGroup, gatewayGroup));

        underTestSpy.fillInstanceMetadata(environmentResponse, stack);
    }

    @Test
    void fillInstanceMetadataTestWhenSubnetAndAvZAndRackIdAndRoundRobinForDLNoAuxiliary() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        InstanceGroupNetwork network = new InstanceGroupNetwork();
        network.setCloudPlatform("aws");
        network.setAttributes(Json.silent(Map.of(SUBNET_IDS, List.of("SUB_1", "SUB_2", "SUB_3"))));
        InstanceGroup masterGroup = getARequestGroup("master", 2, InstanceGroupType.CORE);
        masterGroup.setInstanceGroupNetwork(network);
        InstanceGroup gatewayGroup = getARequestGroup("gateway", 2, InstanceGroupType.GATEWAY);
        gatewayGroup.setInstanceGroupNetwork(network);
        InstanceGroup coreGroup = getARequestGroup("core", 3, InstanceGroupType.CORE);
        coreGroup.setInstanceGroupNetwork(network);
        InstanceGroup idbrokerGroup = getARequestGroup("idbroker", 2, InstanceGroupType.CORE);
        idbrokerGroup.setInstanceGroupNetwork(network);
        stack.setInstanceGroups(Set.of(masterGroup, coreGroup, idbrokerGroup, gatewayGroup));

        underTestSpy.fillInstanceMetadata(environmentResponse, stack);
    }

    @Test
    void fillInstanceMetadataTestWhenSubnetAndAvZAndRackIdAndRoundRobinForLightDutyDL() throws IOException {
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        stack.setCluster(createSdxCluster(SdxClusterShape.LIGHT_DUTY));
        InstanceGroupNetwork network = new InstanceGroupNetwork();
        network.setCloudPlatform("aws");
        network.setAttributes(Json.silent(Map.of(SUBNET_IDS, List.of("SUB_1", "SUB_2", "SUB_3"))));
        InstanceGroup masterGroup = getARequestGroup("master", 2, InstanceGroupType.CORE);
        masterGroup.setInstanceGroupNetwork(network);
        InstanceGroup idbrokerGroup = getARequestGroup("idbroker", 2, InstanceGroupType.CORE);
        idbrokerGroup.setInstanceGroupNetwork(network);
        stack.setInstanceGroups(Set.of(masterGroup, idbrokerGroup));
        underTestSpy.fillInstanceMetadata(environmentResponse, stack);
    }

    @Test
    void fillInstanceMetadataTestWhenSubnetAndAvZAndRackIdAndRoundRobinForMediumDutyDL() throws IOException {
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        stack.setCluster(createSdxCluster(SdxClusterShape.MEDIUM_DUTY_HA));
        InstanceGroupNetwork network = new InstanceGroupNetwork();
        network.setCloudPlatform("aws");
        network.setAttributes(Json.silent(Map.of(SUBNET_IDS, List.of("SUB_1", "SUB_2", "SUB_3"))));
        InstanceGroup masterGroup = getARequestGroup("master", 2, InstanceGroupType.CORE);
        masterGroup.setInstanceGroupNetwork(network);
        InstanceGroup idbrokerGroup = getARequestGroup("idbroker", 2, InstanceGroupType.CORE);
        idbrokerGroup.setInstanceGroupNetwork(network);
        stack.setInstanceGroups(Set.of(masterGroup, idbrokerGroup));
        underTestSpy.fillInstanceMetadata(environmentResponse, stack);
    }

    private Map<String, String> subnetAzPairs(int subnetCount) {
        Map<String, String> subnetAzPairs = new HashMap<>();
        for (int i = 0; i < subnetCount; i++) {
            subnetAzPairs.put(cloudSubnetName(i), cloudSubnetAz(i));
        }
        return subnetAzPairs;
    }

    private String cloudSubnetName(int i) {
        return "name-" + i;
    }

    private String cloudSubnetAz(int i) {
        return "az-" + i;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fillInstanceMetadataTestWhenSubnetAndAvailabilityZoneAndRackIdAndRoundRobinDataProvider")
    void fillInstanceMetadataTestWhenSubnetAndAvailabilityZoneAndRackIdAndRoundRobin(String testCaseName, String subnetId, String availabilityZone) {
        Stack stack = new Stack();
        InstanceGroup workerGroup = getARequestGroup("worker", 3, InstanceGroupType.CORE);
        stack.setInstanceGroups(Set.of(workerGroup));
        Map<String, String> subnetAzPairs = Map.of();
        underTest.fillInstanceMetadata(environmentResponse, stack);

        Map<String, Set<InstanceMetaData>> hostGroupInstances = stack.getInstanceGroups().stream().collect(
                Collectors.toMap(InstanceGroup::getGroupName, InstanceGroup::getAllInstanceMetaData));

        long privateIdStart = 0L;
        validateInstanceMetadataPrivateId("worker", 3, privateIdStart, hostGroupInstances.get("worker"));
    }

    @Test
    void fillInstanceMetadataTestWhenSubnetAndAvailabilityZoneAndRackIdAndStackFallback() {
        Stack stack = new Stack();
        InstanceGroup workerGroup = getARequestGroup("worker", 3, InstanceGroupType.CORE);
        stack.setInstanceGroups(Set.of(workerGroup));
        Network network = new Network();
        network.setAttributes(Json.silent(Map.of("subnetId", "subnet-1")));
        stack.setNetwork(network);

        underTest.fillInstanceMetadata(environmentResponse, stack);

        Map<String, Set<InstanceMetaData>> hostGroupInstances = stack.getInstanceGroups().stream().collect(
                Collectors.toMap(InstanceGroup::getGroupName, InstanceGroup::getAllInstanceMetaData));

        long privateIdStart = 0L;
        validateInstanceMetadataPrivateId("worker", 3, privateIdStart, hostGroupInstances.get("worker"));
    }

    @Test
    void testDetermineStackTypeBasedOnTheUsedApiShouldReturnWhenDatalakeStackTypeAndNotDistroXRequest() {
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);

        StackType actual = underTest.determineStackTypeBasedOnTheUsedApi(stack, false);

        assertEquals(StackType.DATALAKE, actual);
    }

    @Test
    void testDetermineStackTypeBasedOnTheUsedApiShouldReturnWhenDatalakeStackTypeAndDistroXRequest() {
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);

        StackType actual = underTest.determineStackTypeBasedOnTheUsedApi(stack, true);

        assertEquals(StackType.DATALAKE, actual);
    }

    @Test
    void testDetermineStackTypeBasedOnTheUsedApiShouldReturnWhenWorkloadStackTypeAndDistroXRequest() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);

        StackType actual = underTest.determineStackTypeBasedOnTheUsedApi(stack, true);

        assertEquals(StackType.WORKLOAD, actual);
    }

    @Test
    void testDetermineStackTypeBasedOnTheUsedApiShouldReturnWhenWorkloadStackTypeAndNotDistroXRequest() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);

        StackType actual = underTest.determineStackTypeBasedOnTheUsedApi(stack, false);

        assertEquals(StackType.LEGACY, actual);
    }

    @Test
    void testDetermineStackTypeBasedOnTheUsedApiShouldReturnWhenStackTypeIsNullAndNotDistroXRequest() {
        Stack stack = new Stack();
        stack.setType(null);

        StackType actual = underTest.determineStackTypeBasedOnTheUsedApi(stack, false);

        assertEquals(StackType.LEGACY, actual);
    }

    private void validateInstanceMetadataPrivateId(String hostGroup, int nodeCount,
            long privateIdStart, Set<InstanceMetaData> instanceMetaData) {
        assertEquals(nodeCount, instanceMetaData.size(), "Instance Metadata size should match for hostgroup: " + hostGroup);
        for (InstanceMetaData im : instanceMetaData) {
            assertEquals(Long.valueOf(privateIdStart++), im.getPrivateId(), "Private Id should match for hostgroup: " + hostGroup);
        }
    }

    private void validateInstanceMetadataSubnetAndAvailabilityZoneAndRackId(String hostGroup, int nodeCount, Set<InstanceMetaData> instanceMetaData,
            String subnetIdExpected, String availabilityZoneExpected, String rackIdExpected) {
        assertEquals(nodeCount, instanceMetaData.size(), "Instance Metadata size should match for hostgroup: " + hostGroup);
        for (InstanceMetaData im : instanceMetaData) {
            assertThat(im.getSubnetId()).overridingErrorMessage("Subnet Id should match for hostgroup: " + hostGroup).isEqualTo(subnetIdExpected);
            assertThat(im.getAvailabilityZone()).overridingErrorMessage("Availability Zone should match for hostgroup: " + hostGroup)
                    .isEqualTo(availabilityZoneExpected);
            assertThat(im.getRackId()).overridingErrorMessage("Rack Id should match for hostgroup: " + hostGroup).isEqualTo(rackIdExpected);
        }
    }

    private InstanceGroup getARequestGroup(String hostGroup, int numOfNodes, InstanceGroupType hostGroupType) {
        InstanceGroup requestHostGroup = new InstanceGroup();
        requestHostGroup.setGroupName(hostGroup);
        requestHostGroup.setInstanceGroupType(hostGroupType);
        requestHostGroup.setInstanceGroupNetwork(new InstanceGroupNetwork());
        Set<InstanceMetaData> instanceMetadata = new HashSet<>();
        IntStream.range(0, numOfNodes).forEach(count -> instanceMetadata.add(new InstanceMetaData()));
        if ("gateway".equals(hostGroup) || "auxiliary".equals(hostGroup)) {
            instanceMetadata.forEach(metadata -> metadata.setId(1L));
        }
        instanceMetadata.stream()
                .forEach(metadata -> {
                    metadata.setInstanceStatus(SERVICES_RUNNING);
                });
        requestHostGroup.setInstanceMetaData(instanceMetadata);
        return requestHostGroup;
    }

    private Cluster createSdxCluster(SdxClusterShape shape) throws IOException {
        String template = null;
        Blueprint blueprint = new Blueprint();
        switch (shape) {
            case LIGHT_DUTY:
                template = "cdp-sdx";
                blueprint.setDescription("7.2.17 - SDX template with Atlas, HMS, Ranger and other services they are dependent on");
                break;
            case MEDIUM_DUTY_HA:
                template = "cdp-sdx-medium-ha";
                blueprint.setDescription(".2.17 - Medium SDX template with Atlas, HMS, Ranger and other services they are dependent on." +
                        "  Services like HDFS, HBASE, RANGER, HMS have HA");
                break;
            case ENTERPRISE:
                template = "cdp-sdx-enterprise";
                blueprint.setDescription(".2.17 - Enterprise SDX template with Atlas, HMS, Ranger and other services they are dependent on. " +
                        " Services like HDFS, HBASE, RANGER, HMS have HA");
                break;
            case MICRO_DUTY:
                template = "cdp-sdx-micro";
                blueprint.setDescription("7.2.17 - Micro SDX template with Atlas, HMS, Ranger and other services they are dependent on");
                break;
            default:
                template = "cdp-sdx";
        }
        blueprint.setBlueprintText(
                FileReaderUtils.readFileFromPath(
                        Path.of(
                                String.format("../core/src/main/resources/defaults/blueprints/7.2.17/%s.bp", template))));

        Cluster sdxCluster = new Cluster();
        sdxCluster.setBlueprint(blueprint);
        return sdxCluster;
    }
}
