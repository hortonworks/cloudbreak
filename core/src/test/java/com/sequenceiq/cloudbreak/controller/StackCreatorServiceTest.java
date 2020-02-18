package com.sequenceiq.cloudbreak.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.controller.validation.ParametersValidator;
import com.sequenceiq.cloudbreak.controller.validation.filesystem.FileSystemValidator;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateValidator;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.ClusterCreationSetupService;
import com.sequenceiq.cloudbreak.service.StackUnderOperationService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.decorator.StackDecorator;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.State;
import com.sequenceiq.cloudbreak.validation.Validator;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@RunWith(MockitoJUnitRunner.class)
public class StackCreatorServiceTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String INSTANCE_GROUP = "INSTANCE_GROUP";

    private static final String RECIPE_NAME = "RECIPE_NAME";

    private static final String STACK_NAME = "STACK_NAME";

    private static final String CLOUD_PLATFORM = "MOCK";

    private static final String BLUEPRINT_NAME = "BLUEPRINT_NAME";

    private static final String STACK_VERSION = "STACK_VERSION";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
    private Validator<StackV4Request> stackRequestValidator;

    @Mock
    private TransactionService transactionService;

    @Mock
    private StackUnderOperationService stackUnderOperationService;

    @Mock
    private ParametersValidator parametersValidator;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private DatalakeResourcesService datalakeResourcesService;

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
    private Stack stack;

    @Test
    public void shouldThrowBadRequestWhenRequestIsInvalid() {
        User user = new User();
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        StackV4Request stackRequest = new StackV4Request();

        when(validationResult.getState()).thenReturn(State.ERROR);
        when(validationResult.getFormattedErrors()).thenReturn("ERROR_REASON");
        when(stackRequestValidator.validate(stackRequest)).thenReturn(validationResult);

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("ERROR_REASON");

        underTest.createStack(user, workspace, stackRequest);

        verify(blueprintService).updateDefaultBlueprintCollection(WORKSPACE_ID);
        verify(stackRequestValidator).validate(stackRequest);
    }

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

        when(validationResult.getState()).thenReturn(State.VALID);
        when(stackRequestValidator.validate(stackRequest)).thenReturn(validationResult);

        doThrow(new NotFoundException("missing recipe"))
                .when(recipeService).get(NameOrCrn.ofName(RECIPE_NAME), WORKSPACE_ID);

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("The given recipe does not exist for the instance group \"INSTANCE_GROUP\": RECIPE_NAME");

        underTest.createStack(user, workspace, stackRequest);

        verify(blueprintService).updateDefaultBlueprintCollection(WORKSPACE_ID);
        verify(stackRequestValidator).validate(stackRequest);
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

        when(validationResult.getState()).thenReturn(State.VALID);
        when(stackRequestValidator.validate(stackRequest)).thenReturn(validationResult);

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Cluster already exists: STACK_NAME");

        underTest.createStack(user, workspace, stackRequest);

        verify(blueprintService).updateDefaultBlueprintCollection(WORKSPACE_ID);
        verify(stackRequestValidator).validate(stackRequest);
        verify(recipeService).get(NameOrCrn.ofName(RECIPE_NAME), WORKSPACE_ID);
        verify(stackService).getIdByNameInWorkspace(STACK_NAME, WORKSPACE_ID);
    }

    @Test
    public void shouldThrowBadRequestWhenSharedServiceVersionNotMatch() {
        User user = new User();
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        StackV4Request stackRequest = new StackV4Request();
        stackRequest.setName("STACK_NAME");
        InstanceGroupV4Request instanceGroupV4Request = new InstanceGroupV4Request();
        instanceGroupV4Request.setName(INSTANCE_GROUP);
        instanceGroupV4Request.setRecipeNames(Set.of(RECIPE_NAME));
        stackRequest.setInstanceGroups(List.of(instanceGroupV4Request));
        ClusterV4Request cluster = new ClusterV4Request();
        cluster.setBlueprintName(BLUEPRINT_NAME);
        stackRequest.setCluster(cluster);
        SharedServiceV4Request sharedService = new SharedServiceV4Request();
        sharedService.setRuntimeVersion("OTHER_VERSION");
        stackRequest.setSharedService(sharedService);

        when(validationResult.getState()).thenReturn(State.VALID);
        when(stackRequestValidator.validate(stackRequest)).thenReturn(validationResult);
        doThrow(new NotFoundException("stack not found")).when(stackService).getIdByNameInWorkspace(anyString(), anyLong());
        when(converterUtil.convert(stackRequest, Stack.class)).thenReturn(stack);
        when(stack.getCloudPlatform()).thenReturn(CLOUD_PLATFORM);
        Blueprint blueprint = new Blueprint();
        blueprint.setName(BLUEPRINT_NAME);
        blueprint.setStackVersion(STACK_VERSION);
        when(blueprintService.getAllAvailableInWorkspace(any())).thenReturn(Set.of(blueprint));

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Given stack version (STACK_VERSION) does not match with shared context's runtime version (OTHER_VERSION)");

        underTest.createStack(user, workspace, stackRequest);

        verify(blueprintService).updateDefaultBlueprintCollection(WORKSPACE_ID);
        verify(stackRequestValidator).validate(stackRequest);
        verify(recipeService).get(NameOrCrn.ofName(RECIPE_NAME), WORKSPACE_ID);
        verify(stack).setWorkspace(workspace);
        verify(stack).setCreator(user);
        verify(blueprintService).getAllAvailableInWorkspace(workspace);
    }

    @Test
    public void testShouldUseBaseCMImageWithNoCmRequest() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();

        boolean base = underTest.shouldUseBaseCMImage(clusterV4Request);

        assertFalse(base);
    }

    @Test
    public void testShouldUseBaseCMImageWithCmRepo() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        ClouderaManagerV4Request cmRequest = new ClouderaManagerV4Request();
        ClouderaManagerRepositoryV4Request cmRepoRequest = new ClouderaManagerRepositoryV4Request();
        cmRequest.setRepository(cmRepoRequest);
        clusterV4Request.setCm(cmRequest);

        boolean base = underTest.shouldUseBaseCMImage(clusterV4Request);

        assertTrue(base);
    }

    @Test
    public void testShouldUseBaseCMImageWithProducts() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        ClouderaManagerV4Request cmRequest = new ClouderaManagerV4Request();
        ClouderaManagerProductV4Request cdpRequest = new ClouderaManagerProductV4Request();
        cdpRequest.setName("CDP");
        cdpRequest.setParcel("parcel");
        cdpRequest.setVersion("version");
        cdpRequest.setCsd(List.of("csd"));
        cmRequest.setProducts(List.of(cdpRequest));
        clusterV4Request.setCm(cmRequest);

        boolean base = underTest.shouldUseBaseCMImage(clusterV4Request);

        assertTrue(base);
    }

    @Test
    public void testShouldUseBaseCMImageWithProductsAndCmRepo() {
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

        boolean base = underTest.shouldUseBaseCMImage(clusterV4Request);

        assertTrue(base);
    }

}
