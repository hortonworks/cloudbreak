package com.sequenceiq.cloudbreak.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.controller.validation.ParametersValidator;
import com.sequenceiq.cloudbreak.controller.validation.filesystem.FileSystemValidator;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateValidator;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.ClusterCreationSetupService;
import com.sequenceiq.cloudbreak.service.NodeCountLimitValidator;
import com.sequenceiq.cloudbreak.service.StackUnderOperationService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.decorator.StackDecorator;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.multiaz.MultiAzCalculatorService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
public class StackCreatorServiceTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String INSTANCE_GROUP = "INSTANCE_GROUP";

    private static final String RECIPE_NAME = "RECIPE_NAME";

    private static final String STACK_NAME = "STACK_NAME";

    private static final String AWS_PLATFORM = "AWS";

    private static final String YARN_PLATFORM = "YARN";

    @Mock
    private StackDecorator stackDecorator;

    @Mock
    private FileSystemValidator fileSystemValidator;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private ClusterCreationSetupService clusterCreationService;

    @Mock
    private StackService stackService;

    @Mock
    private StackViewService stackViewService;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private ImageService imageService;

    @Mock
    private ConverterUtil converterUtil;

    @Mock
    private TemplateValidator templateValidator;

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
    private EnvironmentClientService environmentClientService;

    @Mock
    private RecipeService recipeService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @InjectMocks
    private StackCreatorService underTest;

    @Mock
    private ValidationResult validationResult;

    @Mock
    private MultiAzCalculatorService multiAzCalculatorService;

    @Mock
    private Stack stack;

    @Mock
    private DetailedEnvironmentResponse environmentResponse;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private NodeCountLimitValidator nodeCountLimitValidator;

    @Test
    public void shouldThrowBadRequestWhenRecipeIsMissing() {
        User user = new User();
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        StackV4Request stackRequest = new StackV4Request();
        InstanceGroupV4Request instanceGroupV4Request = new InstanceGroupV4Request();
        instanceGroupV4Request.setName(INSTANCE_GROUP);
        instanceGroupV4Request.setRecipeNames(Set.of(RECIPE_NAME));
        stackRequest.setInstanceGroups(List.of(instanceGroupV4Request));

        doNothing().when(nodeCountLimitValidator).validateProvision(any());
        doThrow(new NotFoundException("missing recipe"))
                .when(recipeService).get(NameOrCrn.ofName(RECIPE_NAME), WORKSPACE_ID);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.createStack(user, workspace, stackRequest, false));

        assertThat(badRequestException).hasMessage("The given recipe does not exist for the instance group \"INSTANCE_GROUP\": RECIPE_NAME");
    }

    @Test
    public void shouldThrowBadRequestWhenStackNameAlreadyExists() {
        User user = new User();
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        StackV4Request stackRequest = new StackV4Request();
        stackRequest.setName(STACK_NAME);
        InstanceGroupV4Request instanceGroupV4Request = new InstanceGroupV4Request();
        instanceGroupV4Request.setName(INSTANCE_GROUP);
        instanceGroupV4Request.setRecipeNames(Set.of(RECIPE_NAME));
        stackRequest.setInstanceGroups(List.of(instanceGroupV4Request));

        doNothing().when(nodeCountLimitValidator).validateProvision(any());
        when(stackViewService.findByName(anyString(), anyLong())).thenReturn(Optional.of(new StackView()));

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.createStack(user, workspace, stackRequest, false));

        assertThat(badRequestException).hasMessage("Cluster already exists: STACK_NAME");

        verify(recipeService).get(NameOrCrn.ofName(RECIPE_NAME), WORKSPACE_ID);
        verify(stackViewService).findByName(STACK_NAME, WORKSPACE_ID);
    }

    @Test
    public void testShouldUseBaseCMImageShouldReturnFalseWhenCmRequestIsNotPresentAndPlatformIsNotYarn() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();

        boolean actual = underTest.shouldUseBaseCMImage(clusterV4Request, AWS_PLATFORM);

        assertFalse(actual);
    }

    @Test
    public void testShouldUseBaseCMImageShouldReturnTrueWhenCmRequestIsNotPresentAndPlatformIsYarn() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();

        boolean actual = underTest.shouldUseBaseCMImage(clusterV4Request, YARN_PLATFORM);

        assertTrue(actual);
    }

    @Test
    public void testShouldUseBaseCMImageShouldReturnTrueWithCMImageWithCmRepoAndImageIsNotPresentAndPlatformIsNotYarn() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        ClouderaManagerV4Request cmRequest = new ClouderaManagerV4Request();
        ClouderaManagerRepositoryV4Request cmRepoRequest = new ClouderaManagerRepositoryV4Request();
        cmRequest.setRepository(cmRepoRequest);
        clusterV4Request.setCm(cmRequest);

        boolean base = underTest.shouldUseBaseCMImage(clusterV4Request, AWS_PLATFORM);

        assertTrue(base);
    }

    @Test
    public void testShouldUseBaseCMImageShouldReturnTrueWithCMImageWithCmRepoAndImageIsNotPresentAndPlatformIsYarn() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        ClouderaManagerV4Request cmRequest = new ClouderaManagerV4Request();
        ClouderaManagerRepositoryV4Request cmRepoRequest = new ClouderaManagerRepositoryV4Request();
        cmRequest.setRepository(cmRepoRequest);
        clusterV4Request.setCm(cmRequest);

        boolean base = underTest.shouldUseBaseCMImage(clusterV4Request, YARN_PLATFORM);

        assertTrue(base);
    }

    @Test
    public void testShouldUseBaseCMImageWithProductsAndPlatformIsNotYarn() {
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
    public void testShouldUseBaseCMImageWithProductsAndPlatformIsYarn() {
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
    public void testShouldUseBaseCMImageWithProductsAndCmRepoAndPlatformIsNotYarn() {
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
    public void testShouldUseBaseCMImageWithProductsAndCmRepoAndPlatformIsYarn() {
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
    public void testFillInstanceMetadataWhenMaster() {
        Stack stack = new Stack();
        InstanceGroup masterGroup = getARequestGroup("master", 1, InstanceGroupType.GATEWAY);
        InstanceGroup workerGroup = getARequestGroup("worker", 2, InstanceGroupType.CORE);
        InstanceGroup computeGroup = getARequestGroup("compute", 4, InstanceGroupType.CORE);
        stack.setInstanceGroups(Set.of(masterGroup, workerGroup, computeGroup));
        when(multiAzCalculatorService.prepareSubnetAzMap(environmentResponse)).thenReturn(Map.of());
        doNothing().when(multiAzCalculatorService).calculateByRoundRobin(anyMap(), any(InstanceGroup.class));

        underTest.fillInstanceMetadata(environmentResponse, stack);

        Map<String, Set<InstanceMetaData>> hostGroupInstances = stack.getInstanceGroups().stream().collect(
                Collectors.toMap(InstanceGroup::getGroupName, InstanceGroup::getAllInstanceMetaData));
        long privateIdStart = 0L;
        validateInstanceMetadataPrivateId("master", 1, privateIdStart, hostGroupInstances.get("master"));
        validateInstanceMetadataSubnetAndAvailabilityZoneAndRackId("master", 1, hostGroupInstances.get("master"), null, null, null);

        privateIdStart = 1L;
        validateInstanceMetadataPrivateId("compute", 4, privateIdStart, hostGroupInstances.get("compute"));
        validateInstanceMetadataSubnetAndAvailabilityZoneAndRackId("compute", 4, hostGroupInstances.get("compute"), null, null, null);

        privateIdStart = 5L;
        validateInstanceMetadataPrivateId("worker", 2, privateIdStart, hostGroupInstances.get("worker"));
        validateInstanceMetadataSubnetAndAvailabilityZoneAndRackId("worker", 2, hostGroupInstances.get("worker"), null, null, null);
    }

    @Test
    public void testFillInstanceMetadataWhenManager() {
        Stack stack = new Stack();
        InstanceGroup managerGroup = getARequestGroup("manager", 1, InstanceGroupType.GATEWAY);
        InstanceGroup gatewayGroup = getARequestGroup("gateway", 2, InstanceGroupType.CORE);
        InstanceGroup computeGroup = getARequestGroup("compute", 0, InstanceGroupType.CORE);
        InstanceGroup workerGroup = getARequestGroup("worker", 3, InstanceGroupType.CORE);
        InstanceGroup masterGroup = getARequestGroup("master", 2, InstanceGroupType.CORE);
        stack.setInstanceGroups(Set.of(masterGroup, workerGroup, computeGroup, managerGroup, gatewayGroup));
        when(multiAzCalculatorService.prepareSubnetAzMap(environmentResponse)).thenReturn(Map.of());
        doNothing().when(multiAzCalculatorService).calculateByRoundRobin(anyMap(), any(InstanceGroup.class));

        underTest.fillInstanceMetadata(environmentResponse, stack);

        Map<String, Set<InstanceMetaData>> hostGroupInstances = stack.getInstanceGroups().stream().collect(
        Collectors.toMap(InstanceGroup::getGroupName, InstanceGroup::getAllInstanceMetaData));

        long privateIdStart = 0L;
        validateInstanceMetadataPrivateId("manager", 1, privateIdStart, hostGroupInstances.get("manager"));
        validateInstanceMetadataSubnetAndAvailabilityZoneAndRackId("manager", 1, hostGroupInstances.get("manager"), null, null, null);

        privateIdStart = 1L;
        validateInstanceMetadataPrivateId("compute", 0, privateIdStart, hostGroupInstances.get("compute"));
        validateInstanceMetadataSubnetAndAvailabilityZoneAndRackId("compute", 0, hostGroupInstances.get("compute"), null, null, null);

        privateIdStart = 1L;
        validateInstanceMetadataPrivateId("gateway", 2, privateIdStart, hostGroupInstances.get("gateway"));
        validateInstanceMetadataSubnetAndAvailabilityZoneAndRackId("gateway", 2, hostGroupInstances.get("gateway"), null, null, null);

        privateIdStart = 3L;
        validateInstanceMetadataPrivateId("master", 2, privateIdStart, hostGroupInstances.get("master"));
        validateInstanceMetadataSubnetAndAvailabilityZoneAndRackId("master", 2, hostGroupInstances.get("master"), null, null, null);

        privateIdStart = 5L;
        validateInstanceMetadataPrivateId("worker", 3, privateIdStart, hostGroupInstances.get("worker"));
        validateInstanceMetadataSubnetAndAvailabilityZoneAndRackId("worker", 3, hostGroupInstances.get("worker"), null, null, null);
    }

    static Object[][] fillInstanceMetadataTestWhenSubnetAndAvailabilityZoneAndRackIdAndRoundRobinDataProvider() {
        return new Object[][]{
                // testCaseName subnetId availabilityZone
                {"subnetId=\"subnet-1\", availabilityZone=null", "subnet-1", null},
                {"subnetId=\"subnet-1\", availabilityZone=\"az-1\"", "subnet-1", "az-1"},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fillInstanceMetadataTestWhenSubnetAndAvailabilityZoneAndRackIdAndRoundRobinDataProvider")
    public void fillInstanceMetadataTestWhenSubnetAndAvailabilityZoneAndRackIdAndRoundRobin(String testCaseName, String subnetId, String availabilityZone) {
        Stack stack = new Stack();
        InstanceGroup workerGroup = getARequestGroup("worker", 3, InstanceGroupType.CORE);
        stack.setInstanceGroups(Set.of(workerGroup));
        Map<String, String> subnetAzPairs = Map.of();
        when(multiAzCalculatorService.prepareSubnetAzMap(environmentResponse)).thenReturn(subnetAzPairs);
        doAnswer(invocation -> {
            InstanceGroup instanceGroup = invocation.getArgument(1, InstanceGroup.class);
            instanceGroup.getAllInstanceMetaData().forEach(instanceMetaData -> {
                instanceMetaData.setSubnetId(subnetId);
                instanceMetaData.setAvailabilityZone(availabilityZone);
            });
            return null;
        }).when(multiAzCalculatorService).calculateByRoundRobin(subnetAzPairs, workerGroup);
        when(multiAzCalculatorService.determineRackId(subnetId, availabilityZone)).thenReturn("/fooRack");

        underTest.fillInstanceMetadata(environmentResponse, stack);

        Map<String, Set<InstanceMetaData>> hostGroupInstances = stack.getInstanceGroups().stream().collect(
                Collectors.toMap(InstanceGroup::getGroupName, InstanceGroup::getAllInstanceMetaData));

        long privateIdStart = 0L;
        validateInstanceMetadataPrivateId("worker", 3, privateIdStart, hostGroupInstances.get("worker"));
        validateInstanceMetadataSubnetAndAvailabilityZoneAndRackId("worker", 3, hostGroupInstances.get("worker"), subnetId, availabilityZone, "/fooRack");
    }

    @Test
    public void fillInstanceMetadataTestWhenSubnetAndAvailabilityZoneAndRackIdAndStackFallback() {
        Stack stack = new Stack();
        InstanceGroup workerGroup = getARequestGroup("worker", 3, InstanceGroupType.CORE);
        stack.setInstanceGroups(Set.of(workerGroup));
        Network network = new Network();
        network.setAttributes(Json.silent(Map.of("subnetId", "subnet-1")));
        stack.setNetwork(network);
        Map<String, String> subnetAzPairs = Map.of("subnet-1", "az-1");
        when(multiAzCalculatorService.prepareSubnetAzMap(environmentResponse)).thenReturn(subnetAzPairs);
        doNothing().when(multiAzCalculatorService).calculateByRoundRobin(subnetAzPairs, workerGroup);
        when(multiAzCalculatorService.determineRackId("subnet-1", "az-1")).thenReturn("/fooRack");

        underTest.fillInstanceMetadata(environmentResponse, stack);

        Map<String, Set<InstanceMetaData>> hostGroupInstances = stack.getInstanceGroups().stream().collect(
                Collectors.toMap(InstanceGroup::getGroupName, InstanceGroup::getAllInstanceMetaData));

        long privateIdStart = 0L;
        validateInstanceMetadataPrivateId("worker", 3, privateIdStart, hostGroupInstances.get("worker"));
        validateInstanceMetadataSubnetAndAvailabilityZoneAndRackId("worker", 3, hostGroupInstances.get("worker"), "subnet-1", "az-1", "/fooRack");
    }

    @Test
    public void testDetermineStackTypeBasedOnTheUsedApiShouldReturnWhenDatalakeStackTypeAndNotDistroXRequest() {
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);

        StackType actual = underTest.determineStackTypeBasedOnTheUsedApi(stack, false);

        assertEquals(StackType.DATALAKE, actual);
    }

    @Test
    public void testDetermineStackTypeBasedOnTheUsedApiShouldReturnWhenDatalakeStackTypeAndDistroXRequest() {
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);

        StackType actual = underTest.determineStackTypeBasedOnTheUsedApi(stack, true);

        assertEquals(StackType.DATALAKE, actual);
    }

    @Test
    public void testDetermineStackTypeBasedOnTheUsedApiShouldReturnWhenWorkloadStackTypeAndDistroXRequest() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);

        StackType actual = underTest.determineStackTypeBasedOnTheUsedApi(stack, true);

        assertEquals(StackType.WORKLOAD, actual);
    }

    @Test
    public void testDetermineStackTypeBasedOnTheUsedApiShouldReturnWhenWorkloadStackTypeAndNotDistroXRequest() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);

        StackType actual = underTest.determineStackTypeBasedOnTheUsedApi(stack, false);

        assertEquals(StackType.LEGACY, actual);
    }

    @Test
    public void testDetermineStackTypeBasedOnTheUsedApiShouldReturnWhenStackTypeIsNullAndNotDistroXRequest() {
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

        Set<InstanceMetaData> instanceMetadata = new HashSet<>();
        IntStream.range(0, numOfNodes).forEach(count -> instanceMetadata.add(new InstanceMetaData()));
        requestHostGroup.setInstanceMetaData(instanceMetadata);
        return requestHostGroup;
    }

}
