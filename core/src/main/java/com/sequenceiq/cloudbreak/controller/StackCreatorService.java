package com.sequenceiq.cloudbreak.controller;

import static com.sequenceiq.cloudbreak.service.metrics.MetricType.STACK_PREPARATION;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;
import static com.sequenceiq.common.api.type.CdpResourceType.fromStackType;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackValidationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.event.validation.ParametersValidationRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.validation.ParametersValidator;
import com.sequenceiq.cloudbreak.controller.validation.filesystem.FileSystemValidator;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateValidator;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackValidation;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.ClusterCreationSetupService;
import com.sequenceiq.cloudbreak.service.StackUnderOperationService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.decorator.StackDecorator;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.State;
import com.sequenceiq.cloudbreak.validation.Validator;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class StackCreatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreatorService.class);

    @Inject
    private StackDecorator stackDecorator;

    @Inject
    private FileSystemValidator fileSystemValidator;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private ClusterCreationSetupService clusterCreationService;

    @Inject
    private StackService stackService;

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private ImageService imageService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private TemplateValidator templateValidator;

    @Inject
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Inject
    private Validator<StackV4Request> stackRequestValidator;

    @Inject
    private TransactionService transactionService;

    @Inject
    private StackUnderOperationService stackUnderOperationService;

    @Inject
    private ParametersValidator parametersValidator;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    @Inject
    private CredentialClientService credentialClientService;

    @Inject
    @Qualifier("cloudbreakListeningScheduledExecutorService")
    private ExecutorService executorService;

    @Inject
    private CloudbreakMetricService metricService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private RecipeService recipeService;

    @Inject
    private ImageCatalogService imageCatalogService;

    public StackV4Response createStack(User user, Workspace workspace, StackV4Request stackRequest) {
        long start = System.currentTimeMillis();
        blueprintService.updateDefaultBlueprintCollection(workspace.getId());
        LOGGER.info("Validate Stack request.");
        ValidationResult validationResult = stackRequestValidator.validate(stackRequest);
        if (validationResult.getState() == State.ERROR) {
            LOGGER.debug("Stack request has validation error(s): {}.", validationResult.getFormattedErrors());
            throw new BadRequestException(validationResult.getFormattedErrors());
        }

        validateRecipeExistenceOnInstanceGroups(stackRequest.getInstanceGroups(), workspace.getId());

        String stackName = stackRequest.getName();
        LOGGER.info("Check that stack with {} name does not exist.", stackName);
        ensureStackDoesNotExists(stackRequest.getName(), workspace);

        Stack stackStub = measure(() -> converterUtil.convert(stackRequest, Stack.class),
                LOGGER, "Stack request converted to stack in {} ms for stack {}", stackName);
        stackStub.setWorkspace(workspace);
        stackStub.setCreator(user);

        MDCBuilder.buildMdcContext(stackStub);

        String platformString = stackStub.getCloudPlatform().toLowerCase();
        LOGGER.info("Determine blueprint for stack with {} name ", stackName);
        Blueprint blueprint = determineBlueprint(stackRequest, workspace);
        LOGGER.info("Determine image for stack with {} name ", stackName);
        Future<StatedImage> imgFromCatalogFuture = determineImageCatalog(stackName, platformString, stackRequest, blueprint, user, workspace);

        Stack savedStack;
        try {
            savedStack = transactionService.required(() -> {
                LOGGER.info("Decorate stack with {} name ", stackName);
                Stack stack = stackDecorator.decorate(stackStub, stackRequest, user, workspace);

                LOGGER.info("Get credential from environment service with {} environmentCrn.", stack.getEnvironmentCrn());
                Credential credential = credentialClientService.getByEnvironmentCrn(stack.getEnvironmentCrn());
                CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);

                LOGGER.info("Validate Stack parameter for {} name.", stackName);
                ParametersValidationRequest parametersValidationRequest = parametersValidator.triggerValidate(stackRequest.getCloudPlatform().name(),
                        cloudCredential, stack.getParameters(), stack.getCreator().getUserId(), stack.getWorkspace().getId());

                if (stack.getOrchestrator() != null && stack.getOrchestrator().getApiEndpoint() != null) {
                    LOGGER.info("Validate orchestrator for {} name.", stackName);
                    stackService.validateOrchestrator(stack.getOrchestrator());
                }

                measure(() -> {
                    for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                        LOGGER.info("Validate template for {} name with {} instanceGroup.", stackName, instanceGroup.toString());
                        StackType type = stack.getType();
                        templateValidator.validateTemplateRequest(credential,
                                instanceGroup.getTemplate(),
                                stack.getRegion(),
                                stack.getAvailabilityZone(),
                                stack.getPlatformVariant(),
                                fromStackType(type == null ? null : type.name()));
                    }
                }, LOGGER, "Stack's instance templates have been validated in {} ms for stack {}", stackName);

                DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stackRequest.getEnvironmentCrn());
                stack.setUseCcm(environment.getTunnel().useCcm());
                stack.setTunnel(environment.getTunnel());

                if (stackRequest.getCluster() != null) {
                    LOGGER.info("Cluster not null so creating cluster for stack name {}.", stackName);
                    StackValidationV4Request stackValidationRequest = measure(() -> converterUtil.convert(stackRequest, StackValidationV4Request.class),
                            LOGGER, "Stack validation request has been created in {} ms for stack {}", stackName);

                    StackValidation stackValidation = measure(() -> converterUtil.convert(stackValidationRequest, StackValidation.class),
                            LOGGER, "Stack validation object has been created in {} ms for stack {}", stackName);

                    LOGGER.info("Validate Datalake related properties for stack {}.", stackName);
                    setStackTypeAndValidateDatalake(stack, blueprint);

                    stackService.validateStack(stackValidation);

                    LOGGER.info("Validate Filesystem for stack {}.", stackName);
                    fileSystemValidator.validateFileSystem(stackValidation.getCredential().cloudPlatform(), cloudCredential,
                            stackValidationRequest.getFileSystem(), stack.getCreator().getUserId(), stack.getWorkspace().getId());

                    clusterCreationService.validate(stackRequest.getCluster(), cloudCredential, stack, user, workspace, environment);
                }

                LOGGER.info("Fill up instanceMetadata for stack {}.", stackName);
                fillInstanceMetadata(stack);

                parametersValidator.waitResult(parametersValidationRequest);

                LOGGER.info("Get image catalog for stack {}.", stackName);
                StatedImage imgFromCatalog = getImageCatalog(imgFromCatalogFuture);
                Stack newStack = stackService.create(stack, platformString, imgFromCatalog, user, workspace);

                try {
                    LOGGER.info("Create cluster enity in the database with name {}.", stackName);
                    createClusterIfNeed(user, stackRequest, newStack, stackName, blueprint);
                } catch (CloudbreakImageNotFoundException | IOException | TransactionExecutionException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                LOGGER.info("Shared service preparation if required with name {}.", stackName);
                return prepareSharedServiceIfNeed(newStack);
            });
        } catch (TransactionExecutionException e) {
            stackUnderOperationService.off();
            if (e.getCause() instanceof DataIntegrityViolationException) {
                String msg = String.format("Error with resource [%s], error: [%s]", APIResourceType.STACK,
                        getProperSqlErrorMessage((DataIntegrityViolationException) e.getCause()));
                throw new BadRequestException(msg);
            }
            throw new TransactionRuntimeExecutionException(e);
        }

        StackV4Response response = measure(() -> converterUtil.convert(savedStack, StackV4Response.class),
                LOGGER, "Stack response has been created in {} ms for stack {}", savedStack.getName());

        LOGGER.info("Generated stack response after creation: {}", JsonUtil.writeValueAsStringSilentSafe(response));

        flowManager.triggerProvisioning(savedStack.getId());

        metricService.submit(STACK_PREPARATION, System.currentTimeMillis() - start);

        return response;
    }

    private void validateRecipeExistenceOnInstanceGroups(final List<InstanceGroupV4Request> instanceGroupV4Requests, long workspaceId) {
        Map<String, Set<String>> hostGroupRecipeNamePairs = new LinkedHashMap<>(instanceGroupV4Requests.size());

        instanceGroupV4Requests.forEach(instanceGroupV4Request ->
                hostGroupRecipeNamePairs.put(instanceGroupV4Request.getName(), getRecipeNamesIfExists(instanceGroupV4Request)));

        hostGroupRecipeNamePairs.forEach((instanceGroupName, recipeNamesForInstanceGroup) -> {
            Set<String> missingRecipes = collectMissingRecipes(recipeNamesForInstanceGroup, workspaceId);
            throwBadRequestIfHaveMissingRecipe(missingRecipes, instanceGroupName);
        });
    }

    private Set<String> collectMissingRecipes(final Set<String> recipeNames, long workspaceId) {
        Set<String> missingRecipes = new LinkedHashSet<>();
        recipeNames.forEach(recipeName -> {
            try {
                recipeService.get(NameOrCrn.ofName(recipeName), workspaceId);
            } catch (NotFoundException ignore) {
                missingRecipes.add(recipeName);
            }
        });
        return missingRecipes;
    }

    private Set<String> getRecipeNamesIfExists(final InstanceGroupV4Request instanceGroupV4Request) {
        return Optional.ofNullable(instanceGroupV4Request.getRecipeNames()).orElse(new HashSet<>());
    }

    private void throwBadRequestIfHaveMissingRecipe(final Set<String> missingRecipes, final String instanceGroupName) {
        if (!missingRecipes.isEmpty()) {
            if (missingRecipes.size() > 1) {
                throw new BadRequestException(String.format("The given recipes does not exists for the instance group \"%s\": %s",
                        instanceGroupName, String.join(", ", missingRecipes)));
            } else {
                throw new BadRequestException(String.format("The given recipe does not exist for the instance group \"%s\": %s",
                        instanceGroupName, missingRecipes.stream().findFirst().get()));
            }
        }
    }

    private void setStackTypeAndValidateDatalake(Stack stack, Blueprint blueprint) {
        if (blueprintService.isDatalakeBlueprint(blueprint)) {
            stack.setType(StackType.DATALAKE);
            if (stack.getEnvironmentCrn() != null) {
                LOGGER.info("Get datalake count in environment {}", stack.getEnvironmentCrn());
                Long datalakesInEnv = datalakeResourcesService.countDatalakeResourcesInEnvironment(stack.getEnvironmentCrn());
                if (datalakesInEnv >= 1L) {
                    throw new BadRequestException("Only 1 datalake cluster / environment is allowed.");
                }
            }
        } else if (stack.getType() == null) {
            stack.setType(StackType.WORKLOAD);
        }
    }

    private void fillInstanceMetadata(Stack stack) {
        long privateIdNumber = 0;
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getAllInstanceMetaData()) {
                instanceMetaData.setPrivateId(privateIdNumber++);
                instanceMetaData.setInstanceStatus(InstanceStatus.REQUESTED);
            }
        }
    }

    boolean shouldUseBaseCMImage(ClusterV4Request clusterRequest) {
        ClouderaManagerV4Request cmRequest = clusterRequest.getCm();
        return (cmRequest != null && !CollectionUtils.isEmpty(cmRequest.getProducts())) || (cmRequest != null && cmRequest.getRepository() != null);
    }

    private Stack prepareSharedServiceIfNeed(Stack stack) {
        if (stack.getDatalakeResourceId() != null) {
            return sharedServiceConfigProvider.prepareDatalakeConfigs(stack);
        }
        return stack;
    }

    private void createClusterIfNeed(User user, StackV4Request stackRequest, Stack stack, String stackName,
            Blueprint blueprint) throws CloudbreakImageNotFoundException, IOException, TransactionExecutionException {
        if (stackRequest.getCluster() != null) {
            long start = System.currentTimeMillis();
            Cluster cluster = clusterCreationService.prepare(stackRequest.getCluster(), stack, blueprint, user);
            LOGGER.debug("Cluster object and its dependencies has been created in {} ms for stack {}", System.currentTimeMillis() - start, stackName);
            stack.setCluster(cluster);
        }
    }

    private void ensureStackDoesNotExists(String stackName, Workspace workspace) {
        try {
            stackService.getIdByNameInWorkspace(stackName, workspace.getId());
        } catch (NotFoundException e) {
            return;
        }
        throw new BadRequestException("Cluster already exists: " + stackName);
    }

    private Blueprint determineBlueprint(StackV4Request stackRequest, Workspace workspace) {
        if (stackRequest.getCluster() == null) {
            return null;
        }
        Set<Blueprint> blueprints = blueprintService.getAllAvailableInWorkspace(workspace);
        return blueprints.stream()
                .filter(cd -> cd.getName().equals(stackRequest.getCluster().getBlueprintName()))
                .findFirst().orElseThrow(() -> new BadRequestException("Cluster definition not found!"));
    }

    private Future<StatedImage> determineImageCatalog(String stackName, String platformString, StackV4Request stackRequest, Blueprint blueprint,
            User user, Workspace workspace) {
        ClusterV4Request clusterRequest = stackRequest.getCluster();
        if (clusterRequest == null) {
            return null;
        }
        boolean shouldUseBaseCMImage = shouldUseBaseCMImage(clusterRequest);
        boolean baseImageEnabled = imageCatalogService.baseImageEnabled();
        return executorService.submit(() -> {

            LOGGER.info("The stack with name {} has base images enabled: {} and should use base images: {}",
                    stackName, baseImageEnabled, shouldUseBaseCMImage);
            return imageService.determineImageFromCatalog(
                    workspace.getId(),
                    stackRequest.getImage(),
                    platformString,
                    blueprint,
                    shouldUseBaseCMImage,
                    baseImageEnabled,
                    user,
                    image -> true);
        });
    }

    private StatedImage getImageCatalog(Future<StatedImage> imgFromCatalogFuture) {
        int time = 1;
        TimeUnit unit = TimeUnit.MINUTES;
        return Optional.ofNullable(imgFromCatalogFuture).map(f -> {
            try {
                return f.get(time, unit);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof CloudbreakImageNotFoundException) {
                    throw new BadRequestException("Image id settings are incorrect: " + e.getMessage(), e);
                }
                if (e.getCause() instanceof CloudbreakImageCatalogException) {
                    throw new BadRequestException("Image or image catalog settings are incorrect: " + ExceptionUtils.getRootCauseMessage(e), e.getCause());
                }
                throw new RuntimeException("Unknown error happened when determining image from image catalog:" + e.getMessage(), e);
            } catch (InterruptedException e) {
                throw new RuntimeException("Determining image from image catalog interrupted", e);
            } catch (TimeoutException e) {
                throw new RuntimeException(String.format("Could not determine image from image catalog in %d %s", time, unit), e);
            }
        }).orElse(null);
    }

}
