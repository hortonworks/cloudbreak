package com.sequenceiq.cloudbreak.controller;

import static com.sequenceiq.cloudbreak.service.metrics.MetricType.STACK_PREPARATION;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.io.IOException;
import java.util.Comparator;
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
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hue.HueRoles;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.validation.stack.StackRuntimeVersionValidator;
import com.sequenceiq.cloudbreak.converter.IdBrokerConverterUtil;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.IdBroker;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.ClusterCreationSetupService;
import com.sequenceiq.cloudbreak.service.StackUnderOperationService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.decorator.StackDecorator;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.idbroker.IdBrokerService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.validation.HueWorkaroundValidatorService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class StackCreatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreatorService.class);

    @Inject
    private StackDecorator stackDecorator;

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
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Inject
    private TransactionService transactionService;

    @Inject
    private StackUnderOperationService stackUnderOperationService;

    @Inject
    private BlueprintService blueprintService;

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

    @Inject
    private StackViewService stackViewService;

    @Inject
    private OwnerAssignmentService ownerAssignmentService;

    @Inject
    private CredentialConverter credentialConverter;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private StackRuntimeVersionValidator stackRuntimeVersionValidator;

    @Inject
    private HueWorkaroundValidatorService hueWorkaroundValidatorService;

    @Inject
    private IdBrokerConverterUtil idBrokerConverterUtil;

    @Inject
    private IdBrokerService idBrokerService;

    public StackV4Response createStack(User user, Workspace workspace, StackV4Request stackRequest, boolean distroxRequest) {
        long start = System.currentTimeMillis();
        String stackName = stackRequest.getName();

        measure(() ->
                validateRecipeExistenceOnInstanceGroups(stackRequest.getInstanceGroups(), workspace.getId()),
                LOGGER,
                "Check that recipes do exist took {} ms");

        measure(() ->
                ensureStackDoesNotExists(stackName, workspace),
                LOGGER,
                "Stack does not exist check took {} ms");

        Stack stackStub = measure(() ->
                converterUtil.convert(stackRequest, Stack.class),
                LOGGER,
                "Stack request converted to stack took {} ms for stack {}", stackName);

        stackStub.setWorkspace(workspace);
        stackStub.setCreator(user);
        StackType stackType = determineStackTypeBasedOnTheUsedApi(stackStub, distroxRequest);
        stackStub.setType(stackType);
        String platformString = stackStub.getCloudPlatform().toLowerCase();

        MDCBuilder.buildMdcContext(stackStub);
        Stack savedStack;
        try {
            Blueprint blueprint = measure(() ->
                determineBlueprint(stackRequest, workspace),
                LOGGER,
                "Stack request converted to stack took {} ms for stack {}", stackName);
            Future<StatedImage> imgFromCatalogFuture = determineImageCatalog(stackName, platformString, stackRequest, blueprint, user, workspace);
            hueWorkaroundValidatorService.validateForStackRequest(getHueHostGroups(blueprint.getBlueprintText()), stackStub.getName());

            savedStack = transactionService.required(() -> {
                Stack stack = measure(() ->
                        stackDecorator.decorate(stackStub, stackRequest, user, workspace),
                        LOGGER,
                        "Decorate Stack with data took {} ms");

                DetailedEnvironmentResponse environment =  measure(() ->
                        ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                                environmentClientService.getByCrn(stack.getEnvironmentCrn())),
                        LOGGER,
                        "Get Environment from Environment service took {} ms");
                Credential credential = credentialConverter.convert(environment.getCredential());
                CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);


                if (stack.getOrchestrator() != null && stack.getOrchestrator().getApiEndpoint() != null) {
                    measure(() -> stackService.validateOrchestrator(stack.getOrchestrator()),
                            LOGGER,
                            "Validate orchestrator took {} ms");
                }
                stack.setUseCcm(environment.getTunnel().useCcm());
                stack.setTunnel(environment.getTunnel());
                if (stackRequest.getCluster() != null) {
                    measure(() -> setStackType(stack, blueprint),
                        LOGGER,
                        "Set stacktype for stack object took {} ms");

                    measure(() -> clusterCreationService.validate(
                            stackRequest.getCluster(), cloudCredential, stack, user, workspace, environment),
                            LOGGER,
                            "Validate cluster rds and autotls took {} ms");
                }

                measure(() -> fillInstanceMetadata(stack),
                        LOGGER,
                        "Fill up instance metadata took {} ms");


                StatedImage imgFromCatalog = measure(() -> getImageCatalog(imgFromCatalogFuture),
                        LOGGER,
                        "Select the correct image took {} ms");
                stackRuntimeVersionValidator.validate(stackRequest, imgFromCatalog.getImage(), stackType);
                Stack newStack = measure(() -> stackService.create(
                            stack, platformString, imgFromCatalog, user, workspace, Optional.ofNullable(stackRequest.getResourceCrn())),
                            LOGGER,
                        "Save the remaining stack data took {} ms"
                        );

                try {
                    LOGGER.info("Create cluster entity in the database with name {}.", stackName);
                    long clusterSaveStart = System.currentTimeMillis();
                    createClusterIfNeed(user, stackRequest, newStack, stackName, blueprint, environment.getParentEnvironmentCloudPlatform());
                    LOGGER.info("Cluster save took {} ms", System.currentTimeMillis() - clusterSaveStart);
                } catch (CloudbreakImageCatalogException | IOException | TransactionExecutionException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }

                measure(() -> {
                    IdBroker idBroker = idBrokerConverterUtil.generateIdBrokerSignKeys(newStack.getCluster());
                    idBrokerService.save(idBroker);
                }, LOGGER, "Generate id broker sign keys and save");

                Stack withSharedServicesIfNeeded = measure(() -> prepareSharedServiceIfNeed(newStack),
                        LOGGER,
                        "Shared service preparation if required took {} ms with name {}.", stackName);
                measure(() -> assignOwnerRoleOnDataHub(user, stackRequest, newStack),
                        LOGGER,
                        "assignOwnerRoleOnDataHub to stack took {} ms with name {}.", stackName);
                return withSharedServicesIfNeeded;
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
                LOGGER, "Stack response has been created for stack took {} ms with name {}", stackName);

        LOGGER.info("Generated stack response after creation: {}", JsonUtil.writeValueAsStringSilentSafe(response));

        FlowIdentifier flowIdentifier = measure(() ->
                flowManager.triggerProvisioning(savedStack.getId()),
                LOGGER,
                "Stack triggerProvisioning took {} ms with name {}", stackName);
        response.setFlowIdentifier(flowIdentifier);

        metricService.submit(STACK_PREPARATION, System.currentTimeMillis() - start);

        return response;
    }

    private Set<String> getHueHostGroups(String blueprintText) {
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(blueprintText);
        return cmTemplateProcessor.getHostGroupsWithComponent(HueRoles.HUE_SERVER);
    }

    StackType determineStackTypeBasedOnTheUsedApi(Stack stack, boolean distroxRequest) {
        StackType stackType = stack.getType();
        boolean stackTypeIsWorkloadAndNotDistroXRequest = stack.getType() != null && StackType.WORKLOAD.equals(stack.getType()) && !distroxRequest;
        boolean stackTypeIsNullAndNotDistroXRequest = stack.getType() == null && !distroxRequest;
        if (stackTypeIsWorkloadAndNotDistroXRequest || stackTypeIsNullAndNotDistroXRequest) {
            stackType = StackType.LEGACY;
        }
        return stackType;
    }

    private void assignOwnerRoleOnDataHub(User user, StackV4Request stackRequest, Stack newStack) {
        if (StackType.WORKLOAD.equals(stackRequest.getType())) {
            ownerAssignmentService.assignResourceOwnerRoleIfEntitled(user.getUserCrn(), newStack.getResourceCrn(), ThreadBasedUserCrnProvider.getAccountId());
        }
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

    private void setStackType(Stack stack, Blueprint blueprint) {
        if (blueprintService.isDatalakeBlueprint(blueprint)) {
            stack.setType(StackType.DATALAKE);
        } else if (stack.getType() == null) {
            stack.setType(StackType.WORKLOAD);
        }
    }

    void fillInstanceMetadata(Stack stack) {
        long privateIdNumber = 0;
        //Gateway HostGroups are sorted first to start with privateIdNumber 0.
        List<InstanceGroup> sortedInstanceGroups = stack.getInstanceGroups().stream()
                .sorted(Comparator.comparing(InstanceGroup::getInstanceGroupType)
                        .thenComparing(InstanceGroup::getGroupName)).collect(Collectors.toList());
        for (InstanceGroup instanceGroup : sortedInstanceGroups) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getAllInstanceMetaData()) {
                instanceMetaData.setPrivateId(privateIdNumber++);
                instanceMetaData.setInstanceStatus(InstanceStatus.REQUESTED);
            }
        }
    }

    private Stack prepareSharedServiceIfNeed(Stack stack) {
        if (stack.getDatalakeCrn() != null || stack.getDatalakeResourceId() != null) {
            return sharedServiceConfigProvider.prepareDatalakeConfigs(stack);
        }
        LOGGER.debug("No Data Lake resource id has been given for stack! (crn: {})", stack.getResourceCrn());
        return stack;
    }

    private void createClusterIfNeed(User user, StackV4Request stackRequest, Stack stack, String stackName,
            Blueprint blueprint, String parentEnvironmentCloudPlatform) throws CloudbreakImageCatalogException, IOException, TransactionExecutionException {
        if (stackRequest.getCluster() != null) {
            long start = System.currentTimeMillis();
            Cluster cluster = clusterCreationService.prepare(stackRequest.getCluster(), stack, blueprint, user, parentEnvironmentCloudPlatform);
            LOGGER.debug("Cluster object and its dependencies has been created in {} ms for stack {}", System.currentTimeMillis() - start, stackName);
            stack.setCluster(cluster);
        }
    }

    private void ensureStackDoesNotExists(String stackName, Workspace workspace) {
        Optional<StackView> byName = stackViewService.findByName(stackName, workspace.getId());
        if (byName.isPresent()) {
            throw new BadRequestException("Cluster already exists: " + stackName);
        }
    }

    private Blueprint determineBlueprint(StackV4Request stackRequest, Workspace workspace) {
        blueprintService.updateDefaultBlueprintCollection(workspace.getId());
        if (stackRequest.getCluster() == null) {
            return null;
        }
        Set<Blueprint> blueprints = blueprintService.getAllAvailableInWorkspace(workspace);
        String bpName = stackRequest.getCluster().getBlueprintName();
        return blueprints.stream()
                .filter(cd -> cd.getName().equals(bpName))
                .findFirst().orElseThrow(() -> new BadRequestException(String.format("Cluster definition with name %s not found!", bpName)));
    }

    private Future<StatedImage> determineImageCatalog(String stackName, String platformString, StackV4Request stackRequest, Blueprint blueprint,
            User user, Workspace workspace) {
        ClusterV4Request clusterRequest = stackRequest.getCluster();
        if (clusterRequest == null) {
            return null;
        }
        boolean shouldUseBaseCMImage = shouldUseBaseCMImage(clusterRequest, platformString);
        boolean baseImageEnabled = imageCatalogService.baseImageEnabled();
        Map<String, String> mdcContext = MDCBuilder.getMdcContextMap();
        CloudbreakUser cbUser = restRequestThreadLocalService.getCloudbreakUser();

        return executorService.submit(() -> {
            MDCBuilder.buildMdcContextFromMap(mdcContext);
            LOGGER.info("The stack with name {} has base images enabled: {} and should use base images: {}",
                    stackName, baseImageEnabled, shouldUseBaseCMImage);
            StatedImage statedImage = ThreadBasedUserCrnProvider.doAs(user.getUserCrn(), () -> {
                try {
                    restRequestThreadLocalService.setCloudbreakUser(cbUser);
                    return imageService.determineImageFromCatalog(
                            workspace.getId(),
                            stackRequest.getImage(),
                            platformString,
                            blueprint,
                            shouldUseBaseCMImage,
                            baseImageEnabled,
                            user,
                            image -> true);
                } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
                    throw new RuntimeException(e);
                }
            });
            MDCBuilder.cleanupMdc();
            return statedImage;
        });
    }

    boolean shouldUseBaseCMImage(ClusterV4Request clusterRequest, String platformString) {
        ClouderaManagerV4Request cmRequest = clusterRequest.getCm();
        return hasCmParcelInfo(cmRequest) || CloudPlatform.YARN.equalsIgnoreCase(platformString);
    }

    private boolean hasCmParcelInfo(ClouderaManagerV4Request cmRequest) {
        return (cmRequest != null && !CollectionUtils.isEmpty(cmRequest.getProducts())) || (cmRequest != null && cmRequest.getRepository() != null);
    }

    private StatedImage getImageCatalog(Future<StatedImage> imgFromCatalogFuture) {
        int time = 1;
        TimeUnit unit = TimeUnit.MINUTES;
        return Optional.ofNullable(imgFromCatalogFuture).map(f -> {
            try {
                return f.get(time, unit);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof RuntimeException) {
                    if (e.getCause().getCause() instanceof CloudbreakImageNotFoundException) {
                        throw new BadRequestException("Image id settings are incorrect: " + e.getMessage(), e);
                    }
                    if (e.getCause().getCause() instanceof CloudbreakImageCatalogException) {
                        throw new BadRequestException("Image or image catalog settings are incorrect: " + ExceptionUtils.getRootCauseMessage(e), e.getCause());
                    }
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
