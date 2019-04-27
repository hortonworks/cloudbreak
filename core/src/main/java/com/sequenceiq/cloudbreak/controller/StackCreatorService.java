package com.sequenceiq.cloudbreak.controller;

import static com.sequenceiq.cloudbreak.service.metrics.MetricType.STACK_PREPARATION;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackValidationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.event.validation.ParametersValidationRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ParametersValidator;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.State;
import com.sequenceiq.cloudbreak.controller.validation.Validator;
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
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.ClusterCreationSetupService;
import com.sequenceiq.cloudbreak.service.StackUnderOperationService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.credential.CredentialPrerequisiteService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.decorator.StackDecorator;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;

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
    private CredentialPrerequisiteService credentialPrerequisiteService;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    @Inject
    private ExecutorService executorService;

    @Inject
    private CloudbreakMetricService metricService;

    public StackV4Response createStack(CloudbreakUser cloudbreakUser, User user, Workspace workspace, StackV4Request stackRequest) {
        long start = System.currentTimeMillis();

        ValidationResult validationResult = stackRequestValidator.validate(stackRequest);
        if (validationResult.getState() == State.ERROR) {
            LOGGER.debug("Stack request has validation error(s): {}.", validationResult.getFormattedErrors());
            throw new BadRequestException(validationResult.getFormattedErrors());
        }

        String stackName = stackRequest.getName();

        ensureStackDoesNotExists(stackRequest.getName(), workspace);

        Stack stackStub = measure(() -> converterUtil.convert(stackRequest, Stack.class),
                LOGGER, "Stack request converted to stack in {} ms for stack {}", stackName);
        stackStub.setWorkspace(workspace);
        stackStub.setCreator(user);

        MDCBuilder.buildMdcContext(stackStub);

        String platformString = stackStub.getCloudPlatform().toLowerCase();
        Blueprint blueprint = determineBlueprint(stackRequest, workspace);
        Future<StatedImage> imgFromCatalogFuture = determineImageCatalog(stackName, platformString, stackRequest, blueprint, user, workspace);

        Stack savedStack;
        try {
            savedStack = transactionService.required(() -> {
                Stack stack = stackDecorator.decorate(stackStub, stackRequest, user, workspace);

                CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(stackStub.getCredential());

                ParametersValidationRequest parametersValidationRequest = parametersValidator.triggerValidate(stackRequest.getCloudPlatform().name(),
                        cloudCredential, stack.getParameters(), stack.getCreator().getUserId(), stack.getWorkspace().getId());

                if (stack.getOrchestrator() != null && stack.getOrchestrator().getApiEndpoint() != null) {
                    stackService.validateOrchestrator(stack.getOrchestrator());
                }

                measure(() -> {
                    for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                        templateValidator.validateTemplateRequest(stack.getCredential(), instanceGroup.getTemplate(), stack.getRegion(),
                                stack.getAvailabilityZone(), stack.getPlatformVariant());
                    }
                }, LOGGER, "Stack's instance templates have been validated in {} ms for stack {}", stackName);

                if (stackRequest.getCluster() != null) {
                    StackValidationV4Request stackValidationRequest = measure(() -> converterUtil.convert(stackRequest, StackValidationV4Request.class),
                            LOGGER, "Stack validation request has been created in {} ms for stack {}", stackName);


                    StackValidation stackValidation = measure(() -> converterUtil.convert(stackValidationRequest, StackValidation.class),
                            LOGGER, "Stack validation object has been created in {} ms for stack {}", stackName);

                    setStackTypeAndValidateDatalake(stack, blueprint);

                    stackService.validateStack(stackValidation);

                    fileSystemValidator.validateFileSystem(stackValidation.getCredential().cloudPlatform(), cloudCredential,
                                stackValidationRequest.getFileSystem(), stack.getCreator().getUserId(), stack.getWorkspace().getId());

                    clusterCreationService.validate(stackRequest.getCluster(), cloudCredential, stack, user, workspace);
                }

                fillInstanceMetadata(stack);

                parametersValidator.waitResult(parametersValidationRequest);

                StatedImage imgFromCatalog = getImageCatalog(imgFromCatalogFuture);

                Stack newStack = stackService.create(stack, platformString, imgFromCatalog, user, workspace);

                decorateWithDatalakeResourceIdFromEnvironment(newStack);
                try {
                    createClusterIfNeed(user, workspace, stackRequest, newStack, stackName, blueprint);
                } catch (CloudbreakImageNotFoundException | IOException | TransactionExecutionException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
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

        flowManager.triggerProvisioning(savedStack.getId());

        metricService.submit(STACK_PREPARATION, System.currentTimeMillis() - start);

        return response;
    }

    private void decorateWithDatalakeResourceIdFromEnvironment(Stack stack) {
        if (stack.getEnvironment() != null && !CollectionUtils.isEmpty(stack.getEnvironment().getDatalakeResources())
                && stack.getEnvironment().getDatalakeResources().size() == 1 && stack.getDatalakeResourceId() == null) {
            stack.setDatalakeResourceId(stack.getEnvironment().getDatalakeResources().stream().findFirst().get().getId());
        }
    }

    private void setStackTypeAndValidateDatalake(Stack stack, Blueprint blueprint) {
        if (blueprintService.isDatalakeBlueprint(blueprint)) {
            stack.setType(StackType.DATALAKE);
            if (stack.getEnvironment() != null) {
                Long datalakesInEnv = datalakeResourcesService.countDatalakeResourcesInEnvironment(stack.getEnvironment());
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

    private boolean shouldUseBaseImage(ClusterV4Request clusterRequest, Blueprint blueprint) {
        return (clusterRequest.getAmbari() != null && clusterRequest.getAmbari().getRepository() != null)
                || (clusterRequest.getAmbari() != null
                    && clusterRequest.getAmbari().getStackRepository() != null
                    && clusterRequest.getAmbari().getStackRepository().customRepoSpecified())
                || blueprintService.isClouderaManagerTemplate(blueprint);
    }

    private Stack prepareSharedServiceIfNeed(Stack stack) {
        if (credentialPrerequisiteService.isCumulusCredential(stack.getCredential().getAttributes()) || stack.getDatalakeResourceId() != null) {
            return sharedServiceConfigProvider.prepareDatalakeConfigs(stack);
        }
        return stack;
    }

    private void createClusterIfNeed(User user, Workspace workspace, StackV4Request stackRequest, Stack stack, String stackName,
            Blueprint blueprint) throws CloudbreakImageNotFoundException, IOException, TransactionExecutionException {
        if (stackRequest.getCluster() != null) {
            long start = System.currentTimeMillis();
            Cluster cluster = clusterCreationService.prepare(stackRequest.getCluster(), stack, blueprint, user);
            LOGGER.debug("Cluster object and its dependencies has been created in {} ms for stack {}", System.currentTimeMillis() - start, stackName);
            stack.setCluster(cluster);
        }
    }

    private void ensureStackDoesNotExists(String stackName, Workspace workspace) {
        stackService.findStackByNameAndWorkspaceId(stackName, workspace.getId()).ifPresent(stack -> {
            throw new BadRequestException("Cluster already exists: " + stackName);
        });
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
        if (stackRequest.getCluster() == null) {
            return null;
        }
        return executorService.submit(() -> {
            try {
                return imageService.determineImageFromCatalog(workspace.getId(), stackRequest.getImage(), platformString, blueprint,
                        shouldUseBaseImage(stackRequest.getCluster(), blueprint), user);
            } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
                throw new BadRequestException(e.getMessage(), e);
            }
        });
    }

    private StatedImage getImageCatalog(Future<StatedImage> imgFromCatalogFuture) {
        return Optional.ofNullable(imgFromCatalogFuture).map(f -> {
            try {
                return f.get(1, TimeUnit.HOURS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new RuntimeException("Image catalog determination failed", e);
            }
        }).orElse(null);
    }

}
