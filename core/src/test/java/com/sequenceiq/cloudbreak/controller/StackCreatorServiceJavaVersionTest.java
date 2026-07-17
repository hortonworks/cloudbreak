package com.sequenceiq.cloudbreak.controller;

import static com.sequenceiq.cloudbreak.constant.GcpConstants.RAZ_AUTHENTICATION_TYPE_CAB;
import static com.sequenceiq.cloudbreak.constant.GcpConstants.RAZ_AUTHENTICATION_TYPE_HMAC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.controller.validation.stack.StackBlueprintValidator;
import com.sequenceiq.cloudbreak.controller.validation.stack.StackCreationRuntimeVersionValidator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackToStackV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackV4RequestToStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.ClusterCreationSetupService;
import com.sequenceiq.cloudbreak.service.NodeCountLimitValidator;
import com.sequenceiq.cloudbreak.service.StackUnderOperationService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.decorator.StackDecorator;
import com.sequenceiq.cloudbreak.service.encryptionprofile.EncryptionProfileService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.java.JavaDefaultVersionCalculator;
import com.sequenceiq.cloudbreak.service.java.JavaVersionValidator;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeValidatorService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackParametersService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.validation.SeLinuxValidationService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.validation.HueWorkaroundValidatorService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.environment.api.v1.environment.model.response.CompactRegionResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class StackCreatorServiceJavaVersionTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:userName";

    private static final String STACK_CRN = "crn:cdp:datahub:us-west-1:" + ACCOUNT_ID + ":cluster:mystack";

    private static final String STACK_NAME = "mystack";

    private static final Long WORKSPACE_ID = 1L;

    @Mock
    private StackDecorator stackDecorator;

    @Mock
    private ClusterCreationSetupService clusterCreationService;

    @Mock
    private StackService stackService;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private ImageService imageService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private StackUnderOperationService stackUnderOperationService;

    @Mock
    private BlueprintService blueprintService;

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

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private OwnerAssignmentService ownerAssignmentService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private StackCreationRuntimeVersionValidator stackCreationRuntimeVersionValidator;

    @Mock
    private HueWorkaroundValidatorService hueWorkaroundValidatorService;

    @Mock
    private NodeCountLimitValidator nodeCountLimitValidator;

    @Mock
    private StackV4RequestToStackConverter stackV4RequestToStackConverter;

    @Mock
    private StackToStackV4ResponseConverter stackToStackV4ResponseConverter;

    @Mock
    private JavaVersionValidator javaVersionValidator;

    @Mock
    private JavaDefaultVersionCalculator javaDefaultVersionCalculator;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Mock
    private RecipeValidatorService recipeValidatorService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private EncryptionProfileService encryptionProfileService;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private SeLinuxValidationService seLinuxValidationService;

    @Mock
    private StackBlueprintValidator stackBlueprintValidator;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private StackParametersService stackParametersService;

    @InjectMocks
    private StackCreatorService underTest;

    private User user;

    private Workspace workspace;

    private Blueprint blueprint;

    private Stack stack;

    private DetailedEnvironmentResponse environment;

    @BeforeEach
    void setUp() throws Exception {
        user = new User();
        user.setUserCrn(USER_CRN);
        Tenant tenant = new Tenant();
        tenant.setName("tenant");
        workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        workspace.setTenant(tenant);

        blueprint = new Blueprint();
        blueprint.setName("bp");
        blueprint.setDefaultBlueprintText("{\"cdhVersion\":\"7.3.1\",\"hostTemplates\":[]}");
        blueprint.setStackVersion("7.3.1");

        stack = new Stack();
        stack.setId(1L);
        stack.setCloudPlatform("AWS");
        stack.setInstanceGroups(Set.of());
        stack.setCluster(new Cluster());

        environment = mock(DetailedEnvironmentResponse.class);
        CompactRegionResponse regions = mock(CompactRegionResponse.class);
        lenient().when(regions.getNames()).thenReturn(List.of("us-west-1"));
        lenient().when(environment.getRegions()).thenReturn(regions);
        lenient().when(environment.getTunnel()).thenReturn(mock());
        lenient().when(environmentClientService.getByCrn(any())).thenReturn(environment);

        when(regionAwareCrnGenerator.generateCrnStringWithUuid(any(), anyString())).thenReturn(STACK_CRN);
        when(stackDtoService.getStackViewByNameOrCrnOpt(any(), anyString())).thenReturn(Optional.empty());
        lenient().when(blueprintService.getCdhVersion(any(), any())).thenReturn("7.3.2");
        lenient().when(entitlementService.isEntitledToUseOS(anyString(), any())).thenReturn(true);
        when(blueprintService.getAllAvailableInWorkspace(any())).thenReturn(Set.of(blueprint));
        when(stackV4RequestToStackConverter.convert(any(), any())).thenReturn(stack);
        when(stackDecorator.decorate(any(), any(), any(), any(), any())).thenReturn(stack);
        when(transactionService.required(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });
        when(encryptionProfileService.getDefaultEncryptionProfileIfRequired(any(), any(), any())).thenReturn(Optional.empty());
        when(imageService.getSupportedImdsVersion(any(), any())).thenReturn(Optional.empty());
        when(stackService.create(any(Stack.class), any(StatedImage.class), any(User.class), any(Workspace.class))).thenReturn(stack);
        when(stackDtoService.getById(anyLong())).thenReturn(mock(com.sequenceiq.cloudbreak.dto.StackDto.class));
        when(stackToStackV4ResponseConverter.convert(any())).thenReturn(
                new com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response());
    }

    @Test
    void prewarmedImageWithRuntimeVersionUsesImageVersion() throws Exception {
        Image image = mock(Image.class);
        when(image.getRuntimeVersion()).thenReturn(Optional.of("7.3.2.30000"));
        when(image.getStackVersion()).thenReturn(Optional.of("7.3.2"));
        lenient().when(image.getArchitecture()).thenReturn("x86_64");
        StatedImage statedImage = StatedImage.statedImage(image, "url", "catalog");
        setupImageFuture(statedImage);
        when(javaDefaultVersionCalculator.calculate(eq(null), eq("7.3.2.30000"))).thenReturn(21);

        StackV4Request stackRequest = createStackRequest("7.3.2-1.cdh7.3.2.p30000.80393083");

        callCreateStack(stackRequest);

        verify(javaDefaultVersionCalculator).calculate(null, "7.3.2.30000");
        verify(javaVersionValidator).validateImage(image, "7.3.2", 21);
        assertEquals(21, stack.getJavaVersion());
    }

    @Test
    void baseImageWithoutRuntimeVersionFallsBackToCdhRequest() throws Exception {
        Image image = mock(Image.class);
        when(image.getRuntimeVersion()).thenReturn(Optional.empty());
        when(image.getStackVersion()).thenReturn(Optional.empty());
        lenient().when(image.getArchitecture()).thenReturn("x86_64");
        StatedImage statedImage = StatedImage.statedImage(image, "url", "catalog");
        setupImageFuture(statedImage);
        when(javaDefaultVersionCalculator.calculate(eq(null), eq("7.3.2.30000"))).thenReturn(21);

        StackV4Request stackRequest = createStackRequest("7.3.2-1.cdh7.3.2.p30000.80393083");

        callCreateStack(stackRequest);

        verify(javaDefaultVersionCalculator).calculate(null, "7.3.2.30000");
        verify(javaVersionValidator).validateImage(image, "7.3.2", 21);
        assertEquals(21, stack.getJavaVersion());
    }

    @Test
    void baseImageWithOlderCdhVersionResolvesToJdk17() throws Exception {
        Image image = mock(Image.class);
        when(image.getRuntimeVersion()).thenReturn(Optional.empty());
        when(image.getStackVersion()).thenReturn(Optional.empty());
        lenient().when(image.getArchitecture()).thenReturn("x86_64");
        StatedImage statedImage = StatedImage.statedImage(image, "url", "catalog");
        setupImageFuture(statedImage);
        when(javaDefaultVersionCalculator.calculate(eq(null), eq("7.3.2.29999"))).thenReturn(17);

        StackV4Request stackRequest = createStackRequest("7.3.2-1.cdh7.3.2.p29999.80393083");

        callCreateStack(stackRequest);

        verify(javaDefaultVersionCalculator).calculate(null, "7.3.2.29999");
        verify(javaVersionValidator).validateImage(image, "7.3.2", 17);
        assertEquals(17, stack.getJavaVersion());
    }

    @Test
    void baseImageWithNoCdhProductFallsBackToBlueprintVersion() throws Exception {
        Image image = mock(Image.class);
        when(image.getRuntimeVersion()).thenReturn(Optional.empty());
        when(image.getStackVersion()).thenReturn(Optional.empty());
        lenient().when(image.getArchitecture()).thenReturn("x86_64");
        StatedImage statedImage = StatedImage.statedImage(image, "url", "catalog");
        setupImageFuture(statedImage);
        when(javaDefaultVersionCalculator.calculate(any(), eq("7.3.1"))).thenReturn(17);

        StackV4Request stackRequest = createStackRequest(null);

        callCreateStack(stackRequest);

        verify(javaDefaultVersionCalculator).calculate(any(), eq("7.3.1"));
        verify(javaVersionValidator).validateImage(image, "7.3.1", 17);
        assertEquals(17, stack.getJavaVersion());
    }

    @Test
    void prewarmedImageVersionTakesPrecedenceOverCdhRequest() throws Exception {
        Image image = mock(Image.class);
        when(image.getRuntimeVersion()).thenReturn(Optional.of("7.3.2.30000"));
        when(image.getStackVersion()).thenReturn(Optional.of("7.3.2"));
        lenient().when(image.getArchitecture()).thenReturn("x86_64");
        StatedImage statedImage = StatedImage.statedImage(image, "url", "catalog");
        setupImageFuture(statedImage);
        when(javaDefaultVersionCalculator.calculate(eq(null), eq("7.3.2.30000"))).thenReturn(21);

        StackV4Request stackRequest = createStackRequest("7.3.2-1.cdh7.3.2.p29000.80393083");

        callCreateStack(stackRequest);

        verify(javaDefaultVersionCalculator).calculate(null, "7.3.2.30000");
        verify(javaVersionValidator).validateImage(image, "7.3.2", 21);
        assertEquals(21, stack.getJavaVersion());
    }

    @Test
    void userSpecifiedJavaVersionIsPassedToCalculator() throws Exception {
        Image image = mock(Image.class);
        when(image.getRuntimeVersion()).thenReturn(Optional.of("7.3.2.30000"));
        when(image.getStackVersion()).thenReturn(Optional.of("7.3.2"));
        lenient().when(image.getArchitecture()).thenReturn("x86_64");
        StatedImage statedImage = StatedImage.statedImage(image, "url", "catalog");
        setupImageFuture(statedImage);
        when(javaDefaultVersionCalculator.calculate(eq(17), eq("7.3.2.30000"))).thenReturn(17);

        StackV4Request stackRequest = createStackRequest("7.3.2-1.cdh7.3.2.p30000.80393083");
        stackRequest.setJavaVersion(17);

        callCreateStack(stackRequest);

        verify(javaDefaultVersionCalculator).calculate(17, "7.3.2.30000");
        verify(javaVersionValidator).validateImage(image, "7.3.2", 17);
        assertEquals(17, stack.getJavaVersion());
    }

    @Test
    void testCreateStackWithGcpRazAuthTypeWhenItsGivenInStack() throws Exception {
        Image image = mock(Image.class);
        when(image.getRuntimeVersion()).thenReturn(Optional.of("7.3.2"));
        when(image.getStackVersion()).thenReturn(Optional.of("7.3.2"));
        lenient().when(image.getArchitecture()).thenReturn("x86_64");
        StatedImage statedImage = StatedImage.statedImage(image, "url", "catalog");
        setupImageFuture(statedImage);
        when(javaDefaultVersionCalculator.calculate(any(), eq("7.3.2"))).thenReturn(17);
        when(clusterCreationService.prepare(any(), any(), any(), any())).thenReturn(stack.getCluster());
        StackV4Request stackRequest = createStackRequest("7.3.2");
        stack.setCloudPlatform("GCP");
        stack.setType(StackType.DATALAKE);
        stack.getCluster().setRangerRazEnabled(true);
        stack.setParameters(new HashMap<>());
        stack.getParameters().put(PlatformParametersConsts.RAZ_AUTHENTICATION_TYPE, "RazAuthType");

        callCreateStack(stackRequest);
        verify(stackParametersService, never()).setStackParameter(any(), any(), any());
    }

    @Test
    void testCreateStackWithoutGcpRazAuthTypeWhenCMRepoBefore713220000() throws Exception {
        Image image = mock(Image.class);
        when(image.getRuntimeVersion()).thenReturn(Optional.of("7.3.2"));
        when(image.getStackVersion()).thenReturn(Optional.of("7.3.2"));
        lenient().when(image.getArchitecture()).thenReturn("x86_64");
        StatedImage statedImage = StatedImage.statedImage(image, "url", "catalog");
        setupImageFuture(statedImage);
        when(javaDefaultVersionCalculator.calculate(any(), eq("7.3.2"))).thenReturn(17);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails((Long) null)).thenReturn(new ClouderaManagerRepo().withVersion("7.3.2"));
        when(clusterCreationService.prepare(any(), any(), any(), any())).thenReturn(stack.getCluster());
        StackV4Request stackRequest = createStackRequest("7.3.2");
        stack.setCloudPlatform("GCP");
        stack.setType(StackType.DATALAKE);
        stack.getCluster().setRangerRazEnabled(true);
        stack.setParameters(new HashMap<>());

        callCreateStack(stackRequest);
        verify(stackParametersService, times(1)).setStackParameter(
                stack.getId(), PlatformParametersConsts.RAZ_AUTHENTICATION_TYPE, RAZ_AUTHENTICATION_TYPE_HMAC);
    }

    @Test
    void testCreateStackWithoutGcpRazAuthTypeWhenCMRepoAfter713220000() throws Exception {
        Image image = mock(Image.class);
        when(image.getRuntimeVersion()).thenReturn(Optional.of("7.3.2"));
        when(image.getStackVersion()).thenReturn(Optional.of("7.3.2"));
        lenient().when(image.getArchitecture()).thenReturn("x86_64");
        StatedImage statedImage = StatedImage.statedImage(image, "url", "catalog");
        setupImageFuture(statedImage);
        when(javaDefaultVersionCalculator.calculate(any(), eq("7.3.2"))).thenReturn(17);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails((Long) null)).thenReturn(new ClouderaManagerRepo().withVersion("7.13.2.20000"));
        when(clusterCreationService.prepare(any(), any(), any(), any())).thenReturn(stack.getCluster());
        StackV4Request stackRequest = createStackRequest("7.3.2");
        stack.setCloudPlatform("GCP");
        stack.setType(StackType.DATALAKE);
        stack.getCluster().setRangerRazEnabled(true);
        stack.setParameters(new HashMap<>());

        callCreateStack(stackRequest);
        verify(stackParametersService, times(1)).setStackParameter(
                stack.getId(), PlatformParametersConsts.RAZ_AUTHENTICATION_TYPE, RAZ_AUTHENTICATION_TYPE_CAB);
    }

    private StackV4Response callCreateStack(StackV4Request stackRequest) {
        return ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createStack(user, workspace, stackRequest, true));
    }

    private void setupImageFuture(StatedImage statedImage) {
        CompletableFuture<StatedImage> future = CompletableFuture.completedFuture(statedImage);
        when(executorService.submit(any(java.util.concurrent.Callable.class))).thenReturn(future);
    }

    private StackV4Request createStackRequest(String cdhVersion) {
        StackV4Request stackRequest = new StackV4Request();
        stackRequest.setName(STACK_NAME);
        stackRequest.setEnvironmentCrn("crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:env1");
        InstanceGroupV4Request instanceGroup = new InstanceGroupV4Request();
        instanceGroup.setName("master");
        stackRequest.setInstanceGroups(List.of(instanceGroup));

        ClusterV4Request clusterRequest = new ClusterV4Request();
        clusterRequest.setBlueprintName("bp");
        ClouderaManagerV4Request cmRequest = new ClouderaManagerV4Request();
        if (cdhVersion != null) {
            ClouderaManagerProductV4Request cdhProduct = new ClouderaManagerProductV4Request();
            cdhProduct.setName("CDH");
            cdhProduct.setVersion(cdhVersion);
            cmRequest.setProducts(List.of(cdhProduct));
        } else {
            cmRequest.setProducts(List.of());
        }
        clusterRequest.setCm(cmRequest);
        stackRequest.setCluster(clusterRequest);
        return stackRequest;
    }
}
