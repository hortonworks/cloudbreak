package com.sequenceiq.cloudbreak.controller;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.service.metrics.MetricType.STACK_PREPARATION;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.BaseSecurityV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.cloud.aws.common.DistroxEnabledInstanceTypes;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hue.HueRoles;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.validation.stack.StackRuntimeVersionValidator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackToStackV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackV4RequestToStackConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.ClusterCreationSetupService;
import com.sequenceiq.cloudbreak.service.NodeCountLimitValidator;
import com.sequenceiq.cloudbreak.service.StackUnderOperationService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.decorator.StackDecorator;
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
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.validation.SeLinuxValidationService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.validation.HueWorkaroundValidatorService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class StackCreatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreatorService.class);

    @Inject
    private StackDecorator stackDecorator;

    @Inject
    private ClusterCreationSetupService clusterCreationService;

    @Inject
    private StackService stackService;

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private ImageService imageService;

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
    private EnvironmentService environmentClientService;

    @Inject
    private RecipeService recipeService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private OwnerAssignmentService ownerAssignmentService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private StackRuntimeVersionValidator stackRuntimeVersionValidator;

    @Inject
    private HueWorkaroundValidatorService hueWorkaroundValidatorService;

    @Inject
    private NodeCountLimitValidator nodeCountLimitValidator;

    @Inject
    private StackV4RequestToStackConverter stackV4RequestToStackConverter;

    @Inject
    private StackToStackV4ResponseConverter stackToStackV4ResponseConverter;

    @Inject
    private JavaVersionValidator javaVersionValidator;

    @Inject
    private JavaDefaultVersionCalculator javaDefaultVersionCalculator;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Inject
    private RecipeValidatorService recipeValidatorService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private SeLinuxValidationService seLinuxValidationService;

    public StackV4Response createStack(User user, Workspace workspace, StackV4Request stackRequest, boolean distroxRequest) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        long start = System.currentTimeMillis();
        String stackName = stackRequest.getName();
        String crn = getCrnForCreation(Optional.ofNullable(stackRequest.getResourceCrn()));

        measure(() -> recipeValidatorService.validateRecipeExistenceOnInstanceGroups(stackRequest.getInstanceGroups(), workspace.getId()),
                LOGGER,
                "Check that recipes do exist took {} ms");

        measure(() -> ensureStackDoesNotExists(stackName),
                LOGGER,
                "Stack does not exist check took {} ms");

        DetailedEnvironmentResponse environment = measure(
                () -> ThreadBasedUserCrnProvider.doAsInternalActor(
                        () -> environmentClientService.getByCrn(stackRequest.getEnvironmentCrn())),
                LOGGER,
                "Get Environment from Environment service took {} ms");
        nodeCountLimitValidator.validateProvision(stackRequest, environment.getRegions().getNames().stream().findFirst().orElse(null));
        validateArchitecture(stackRequest, distroxRequest, workspace.getId());
        validateSeLinuxEntitlement(stackRequest);

        Stack stackStub = measure(
                () -> stackV4RequestToStackConverter.convert(environment, stackRequest),
                LOGGER,
                "Stack request converted to stack took {} ms for stack {}", stackName);

        stackStub.setWorkspace(workspace);
        stackStub.setCreator(user);
        StackType stackType = determineStackTypeBasedOnTheUsedApi(stackStub, distroxRequest);
        stackStub.setType(stackType);
        stackStub.setMultiAz(stackRequest.isEnableMultiAz());
        String platformString = stackStub.getCloudPlatform().toLowerCase(Locale.ROOT);
        measure(() -> assignOwnerRoleOnDataHub(stackType, crn),
                LOGGER,
                "assignOwnerRoleOnDataHub to stack took {} ms with name {}.", stackName);

        MDCBuilder.buildMdcContext(stackStub);
        Stack savedStack;
        try {
            Blueprint blueprint = measure(
                    () -> determineBlueprint(stackRequest, workspace),
                    LOGGER,
                    "Blueprint determined in {} ms for stack {}", stackName);
            Future<StatedImage> imgFromCatalogFuture = determineImage(stackName, platformString, stackRequest, blueprint, user, workspace);
            hueWorkaroundValidatorService.validateForStackRequest(getHueHostGroups(blueprint.getBlueprintJsonText()), stackStub.getName());

            savedStack = transactionService.required(() -> {
                Stack stack = measure(
                        () -> stackDecorator.decorate(environment, stackStub, stackRequest, user, workspace),
                        LOGGER,
                        "Decorate Stack with data took {} ms");

                if (stack.getOrchestrator() != null && stack.getOrchestrator().getApiEndpoint() != null) {
                    measure(() -> stackService.validateOrchestrator(stack.getOrchestrator()),
                            LOGGER,
                            "Validate orchestrator took {} ms");
                }
                stack.setResourceCrn(crn);
                stack.setUseCcm(environment.getTunnel().useCcm());
                stack.setTunnel(environment.getTunnel());
                if (stackRequest.getCluster() != null) {
                    measure(() -> setStackType(stack, blueprint),
                            LOGGER,
                            "Set stacktype for stack object took {} ms");

                    measure(() -> clusterCreationService.validate(stackRequest.getCluster(), stack, user, workspace, environment),
                            LOGGER,
                            "Validate cluster rds and autotls took {} ms");
                }

                measure(() -> fillInstanceMetadata(environment, stack),
                        LOGGER,
                        "Fill up instance metadata took {} ms");


                StatedImage imgFromCatalog = measure(() -> getImageFromCatalog(imgFromCatalogFuture),
                        LOGGER,
                        "Select the correct image took {} ms");
                validateImageAndInstanceTypeArchitectureForAws(stackRequest, imgFromCatalog);
                int javaVersion = javaDefaultVersionCalculator.calculate(stackRequest.getJavaVersion(), blueprint.getStackVersion());
                setJavaVersion(stackRequest, stack, javaVersion);
                javaVersionValidator.validateImage(imgFromCatalog.getImage(), blueprint.getStackVersion(), stackRequest.getJavaVersion());
                stackRuntimeVersionValidator.validate(stackRequest, imgFromCatalog.getImage(), stackType);
                imageService.getSupportedImdsVersion(stack.cloudPlatform(), imgFromCatalog).ifPresent(stack::setSupportedImdsVersion);
                Stack newStack = measure(
                        () -> stackService.create(stack, imgFromCatalog, user, workspace),
                        LOGGER,
                        "Save the remaining stack data took {} ms"
                );
                securityConfigService.validateRequest(stackRequest.getSecurity(), accountId);
                securityConfigService.create(stack, stackRequest.getSecurity());
                seLinuxValidationService.validateSeLinuxSupportedOnTargetImage(stack, imgFromCatalog.getImage());

                try {
                    LOGGER.info("Create cluster entity in the database with name {}.", stackName);
                    long clusterSaveStart = System.currentTimeMillis();
                    createClusterIfNeeded(user, stackRequest, newStack, stackName, blueprint);
                    LOGGER.info("Cluster save took {} ms", System.currentTimeMillis() - clusterSaveStart);
                } catch (CloudbreakImageCatalogException | IOException | TransactionExecutionException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                return newStack;
            });
        } catch (TransactionExecutionException e) {
            ownerAssignmentService.notifyResourceDeleted(crn);
            stackUnderOperationService.off();
            if (e.getCause() instanceof DataIntegrityViolationException) {
                String msg = String.format("Error with resource [%s], error: [%s]", APIResourceType.STACK,
                        getProperSqlErrorMessage((DataIntegrityViolationException) e.getCause()));
                throw new BadRequestException(msg, e.getCause());
            }
            throw new TransactionRuntimeExecutionException(e);
        }

        StackDto stackDto = stackDtoService.getById(savedStack.getId());
        StackV4Response response = measure(() -> stackToStackV4ResponseConverter.convert(stackDto),
                LOGGER, "Stack response has been created for stack took {} ms with name {}", stackName);

        LOGGER.info("Generated stack response after creation: {}", JsonUtil.writeValueAsStringSilentSafe(response));

        FlowIdentifier flowIdentifier = measure(
                () -> flowManager.triggerProvisioning(savedStack.getId()),
                LOGGER,
                "Stack triggerProvisioning took {} ms with name {}", stackName);
        response.setFlowIdentifier(flowIdentifier);

        recipeService.sendClusterCreationUsageReport(savedStack);

        metricService.recordTimer(System.currentTimeMillis() - start, STACK_PREPARATION);

        return response;
    }

    void validateImageAndInstanceTypeArchitectureForAws(StackV4Request stackRequest, StatedImage image) {
        if (CloudPlatform.AWS.equals(stackRequest.getCloudPlatform()) && !StackType.DATALAKE.equals(stackRequest.getType())) {
            Architecture imageArchitecture = Architecture.fromStringWithFallback(image.getImage().getArchitecture());
            List<Pair<String, Architecture>> instanceTypeArchitectures = stackRequest.getInstanceGroups()
                    .stream()
                    .filter(group -> !Objects.isNull(group.getTemplate()) && !Objects.isNull(group.getTemplate().getInstanceType()))
                    .map(group -> {
                        String instanceType = group.getTemplate().getInstanceType();
                        if (DistroxEnabledInstanceTypes.AWS_ENABLED_ARM64_TYPES_LIST.contains(instanceType)) {
                            return Pair.of(instanceType, Architecture.ARM64);
                        } else {
                            return Pair.of(instanceType, Architecture.X86_64);
                        }
                    })
                    .toList();
            instanceTypeArchitectures.forEach(value -> {
                String instanceType = value.getKey();
                Architecture instanceArchitecture = value.getValue();
                if (!Objects.equals(imageArchitecture, instanceArchitecture)) {
                    throw new BadRequestException(String.format(
                            "The selected image %s has different architecture %s than the selected %s instance type's %s architecture.",
                            image.getImage().getUuid(), imageArchitecture.getName(), instanceType, instanceArchitecture.getName()));
                }
            });
        }
    }

    private void setJavaVersion(StackV4Request stackRequest, Stack stack, int javaVersion) {
        stackRequest.setJavaVersion(javaVersion);
        stack.setJavaVersion(javaVersion);
    }

    private void validateArchitecture(StackV4Request stackRequest, boolean distroxRequest, Long workspaceId) {
        if (stackRequest.getArchitectureEnum() == Architecture.ARM64) {
            if (!isCodRequest(stackRequest) && stackRequest.getCluster() != null) {
                String version = blueprintService.getCdhVersion(NameOrCrn.ofName(stackRequest.getCluster().getBlueprintName()), workspaceId);
                if (!isVersionNewerOrEqualThanLimited(version, CLOUDERA_STACK_VERSION_7_3_1)) {
                    throw new BadRequestException(String.format("The selected architecture (%s) is not supported in this cdh version (%s).",
                            Architecture.ARM64.getName(), version));
                }
            }
        }
    }

    private void validateSeLinuxEntitlement(StackV4Request stackRequest) {
        SeLinux seLinuxModeFromRequest = Optional.ofNullable(stackRequest.getSecurity())
                .map(BaseSecurityV4::getSeLinux)
                .map(SeLinux::fromStringWithFallback)
                .orElse(SeLinux.PERMISSIVE);
        seLinuxValidationService.validateSeLinuxEntitlementGranted(seLinuxModeFromRequest);
    }

    private boolean isCodRequest(StackV4Request stackRequest) {
        return Optional.ofNullable(stackRequest.getTags())
                .map(TagsV4Request::getApplication)
                .map(map -> Boolean.parseBoolean(map.get("is_cod_cluster")))
                .orElse(Boolean.FALSE);
    }

    private String getCrnForCreation(Optional<String> externalCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return externalCrn.orElseGet(() -> createCRN(accountId));
    }

    private String createCRN(String accountId) {
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.DATAHUB, accountId);
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

    private void assignOwnerRoleOnDataHub(StackType stackType, String resourceCrn) {
        if (StackType.WORKLOAD.equals(stackType)) {
            ownerAssignmentService.assignResourceOwnerRoleIfEntitled(ThreadBasedUserCrnProvider.getUserCrn(), resourceCrn);
        }
    }

    private void setStackType(Stack stack, Blueprint blueprint) {
        if (blueprintService.isDatalakeBlueprint(blueprint)) {
            stack.setType(StackType.DATALAKE);
        } else if (stack.getType() == null) {
            stack.setType(StackType.WORKLOAD);
        }
    }

    @VisibleForTesting
    void fillInstanceMetadata(DetailedEnvironmentResponse environment, Stack stack) {
        long privateIdNumber = 0;
        for (InstanceGroup instanceGroup : sortInstanceGroups(stack)) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getAllInstanceMetaData()) {
                instanceMetaData.setPrivateId(privateIdNumber++);
                instanceMetaData.setInstanceStatus(InstanceStatus.REQUESTED);
            }
        }
    }

    public List<InstanceGroup> sortInstanceGroups(Stack stack) {
        return stack.getInstanceGroups().stream()
                .sorted(createGatewayFirstComparator()).collect(Collectors.toList());
    }

    private Comparator<InstanceGroup> createGatewayFirstComparator() {
        return Comparator.comparing(InstanceGroup::getInstanceGroupType)
                .thenComparing(InstanceGroup::getGroupName);
    }

    private void createClusterIfNeeded(User user, StackV4Request stackRequest, Stack stack, String stackName, Blueprint blueprint)
            throws CloudbreakImageCatalogException, IOException, TransactionExecutionException {
        if (stackRequest.getCluster() != null) {
            long start = System.currentTimeMillis();
            Cluster cluster = clusterCreationService.prepare(stackRequest.getCluster(), stack, blueprint, user);
            LOGGER.debug("Cluster object and its dependencies has been created in {} ms for stack {}", System.currentTimeMillis() - start, stackName);
            stack.setCluster(cluster);
        }
    }

    private void ensureStackDoesNotExists(String stackName) {
        Optional<StackView> byName = stackDtoService.getStackViewByNameOrCrnOpt(NameOrCrn.ofName(stackName), ThreadBasedUserCrnProvider.getAccountId());
        if (byName.isPresent()) {
            throw new BadRequestException("Cluster already exists: " + stackName);
        }
    }

    private Blueprint determineBlueprint(StackV4Request stackRequest, Workspace workspace) {
        if (stackRequest.getCluster() == null) {
            return null;
        }
        Set<Blueprint> blueprints = blueprintService.getAllAvailableInWorkspace(workspace);
        String bpName = stackRequest.getCluster().getBlueprintName();
        return blueprints.stream()
                .filter(cd -> cd.getName().equals(bpName))
                .findFirst().orElseThrow(() -> new BadRequestException(String.format("Cluster definition with name %s not found!", bpName)));
    }

    private Future<StatedImage> determineImage(String stackName, String platformString, StackV4Request stackRequest, Blueprint blueprint,
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
                            stackRequest.getArchitectureEnum(),
                            platformString,
                            stackRequest.getVariant(),
                            blueprint,
                            shouldUseBaseCMImage,
                            baseImageEnabled,
                            user,
                            image -> true);
                } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
                    throw new RuntimeException(e);
                }
            });
            OsType imageOs = OsType.getByOsTypeStringWithCentos7Fallback(statedImage.getImage().getOsType());
            if (!entitlementService.isEntitledToUseOS(ThreadBasedUserCrnProvider.getAccountId(), imageOs)) {
                throw new BadRequestException(String.format("Your account is not entitled to use %s images.", imageOs.getShortName()));
            }
            MDCBuilder.cleanupMdc();
            return statedImage;
        });
    }

    boolean shouldUseBaseCMImage(ClusterV4Request clusterRequest, String platformString) {
        ClouderaManagerV4Request cmRequest = clusterRequest.getCm();
        return hasCmParcelInfo(cmRequest) || CloudPlatform.YARN.equalsIgnoreCase(platformString);
    }

    private boolean hasCmParcelInfo(ClouderaManagerV4Request cmRequest) {
        return cmRequest != null && !CollectionUtils.isEmpty(cmRequest.getProducts()) || cmRequest != null && cmRequest.getRepository() != null;
    }

    private StatedImage getImageFromCatalog(Future<StatedImage> imgFromCatalogFuture) {
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