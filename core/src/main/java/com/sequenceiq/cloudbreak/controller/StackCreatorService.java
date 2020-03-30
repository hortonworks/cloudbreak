package com.sequenceiq.cloudbreak.controller;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.stack.StackRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackValidationRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.controller.validation.ParametersValidator;
import com.sequenceiq.cloudbreak.controller.validation.StackSensitiveDataPropagator;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.State;
import com.sequenceiq.cloudbreak.controller.validation.Validator;
import com.sequenceiq.cloudbreak.controller.validation.filesystem.FileSystemValidator;
import com.sequenceiq.cloudbreak.controller.validation.stack.PaywallCredentialValidator;
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
import com.sequenceiq.cloudbreak.service.decorator.StackDecorator;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
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
    private StackSensitiveDataPropagator stackSensitiveDataPropagator;

    @Inject
    private ClusterCreationSetupService clusterCreationService;

    @Inject
    private StackService stackService;

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private ImageService imageService;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private TemplateValidator templateValidator;

    @Inject
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Inject
    private Validator<StackRequest> stackRequestValidator;

    @Inject
    private TransactionService transactionService;

    @Inject
    private StackUnderOperationService stackUnderOperationService;

    @Inject
    private ParametersValidator parametersValidator;

    @Inject
    private PaywallCredentialValidator paywallCredentialValidator;

    public StackResponse createStack(CloudbreakUser cloudbreakUser, User user, Workspace workspace, StackRequest stackRequest) {
        ValidationResult validationResult = stackRequestValidator.validate(stackRequest);
        if (validationResult.getState() == State.ERROR) {
            LOGGER.info("Stack request has validation error(s): {}.", validationResult.getFormattedErrors());
            throw new BadRequestException(validationResult.getFormattedErrors());
        }

        Stack savedStack;
        try {
            savedStack = transactionService.required(() -> {
                long start = System.currentTimeMillis();
                ensureStackDoesNotExists(stackRequest.getName(), workspace);

                Stack stack = conversionService.convert(stackRequest, Stack.class);

                stack.setWorkspace(workspace);
                stack.setCreator(user);

                String stackName = stack.getName();
                LOGGER.info("Stack request converted to stack in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                MDCBuilder.buildMdcContext(stack);

                start = System.currentTimeMillis();
                stack = stackSensitiveDataPropagator.propagate(stackRequest.getCredentialSource(), stack, workspace);
                LOGGER.info("Stack propagated with sensitive data in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                start = System.currentTimeMillis();
                stack = stackDecorator.decorate(stack, stackRequest, user, workspace);
                LOGGER.info("Stack object has been decorated in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                if (stack.getOrchestrator() != null && stack.getOrchestrator().getApiEndpoint() != null) {
                    stackService.validateOrchestrator(stack.getOrchestrator());
                }

                start = System.currentTimeMillis();
                for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                    templateValidator.validateTemplateRequest(stack.getCredential(), instanceGroup.getTemplate(), stack.getRegion(),
                            stack.getAvailabilityZone(), stack.getPlatformVariant());
                }
                LOGGER.info("Stack's instance templates have been validated in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(stack.getCredential());
                Blueprint blueprint = null;
                if (stackRequest.getClusterRequest() != null) {
                    start = System.currentTimeMillis();
                    StackValidationRequest stackValidationRequest = conversionService.convert(stackRequest, StackValidationRequest.class);
                    LOGGER.info("Stack validation request has been created in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                    start = System.currentTimeMillis();
                    StackValidation stackValidation = conversionService.convert(stackValidationRequest, StackValidation.class);
                    LOGGER.info("Stack validation object has been created in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                    blueprint = stackValidation.getBlueprint();

                    start = System.currentTimeMillis();
                    paywallCredentialValidator.validateCredential(stackRequest.getClusterRequest(), blueprint.getStackType(), blueprint.getStackVersion());
                    LOGGER.info("Paywall credential has been validated in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                    start = System.currentTimeMillis();
                    stackService.validateStack(stackValidation, stackRequest.getClusterRequest().getValidateBlueprint());
                    LOGGER.info("Stack has been validated in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                    start = System.currentTimeMillis();
                    fileSystemValidator.validateFileSystem(stackValidationRequest.getPlatform(), cloudCredential, stackValidationRequest.getFileSystem(),
                            stack.getCreator().getUserId(), stack.getWorkspace().getId());
                    LOGGER.info("Filesystem has been validated in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                    start = System.currentTimeMillis();
                    clusterCreationService.validate(stackRequest.getClusterRequest(), cloudCredential, stack, user, workspace);
                    LOGGER.info("Cluster has been validated in {} ms for stack {}", System.currentTimeMillis() - start, stackName);
                }

                start = System.currentTimeMillis();
                parametersValidator.validate(stackRequest.getCloudPlatform(), cloudCredential, stack.getParameters(), stack.getCreator().getUserId(),
                        stack.getWorkspace().getId());
                LOGGER.info("Parameter validation has been finished under {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                start = System.currentTimeMillis();
                String platformString = platform(stack.cloudPlatform()).value().toLowerCase();
                StatedImage imgFromCatalog;
                try {
                    imgFromCatalog = imageService.determineImageFromCatalog(workspace.getId(),
                            stackRequest.getImageId(), platformString, stackRequest.getImageCatalog(), blueprint,
                            shouldUseBaseImage(stackRequest.getClusterRequest()), stackRequest.getOs(), cloudbreakUser, user);
                } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                LOGGER.info("Image for stack has been determined under {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                fillInstanceMetadata(stack);

                start = System.currentTimeMillis();
                stack = stackService.create(stack, platformString, imgFromCatalog, user, workspace);
                LOGGER.info("Stack object and its dependencies has been created in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                try {
                    createClusterIfNeed(user, workspace, stackRequest, stack, stackName, blueprint);
                } catch (CloudbreakImageNotFoundException | IOException | TransactionExecutionException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                prepareSharedServiceIfNeed(stackRequest, stack, stackName);
                return stack;
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

        long start = System.currentTimeMillis();
        StackResponse response = conversionService.convert(savedStack, StackResponse.class);
        LOGGER.info("Stack response has been created in {} ms for stack {}", System.currentTimeMillis() - start, savedStack.getName());

        start = System.currentTimeMillis();
        flowManager.triggerProvisioning(savedStack.getId());
        LOGGER.info("Stack provision triggered in {} ms for stack {}", System.currentTimeMillis() - start, savedStack.getName());

        return response;
    }

    private void fillInstanceMetadata(Stack stack) {
        long privateIdNumber = 0;
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getAllInstanceMetaData()) {
                instanceMetaData.setPrivateId(privateIdNumber);
                instanceMetaData.setInstanceStatus(InstanceStatus.REQUESTED);
                privateIdNumber++;
            }
        }
    }

    private boolean shouldUseBaseImage(ClusterRequest clusterRequest) {
        return clusterRequest.getAmbariRepoDetailsJson() != null || clusterRequest.getAmbariStackDetails() != null;
    }

    private Stack prepareSharedServiceIfNeed(StackRequest stackRequest, Stack stack, String stackName) {
        if (stackRequest.getClusterRequest() != null && stackRequest.getClusterRequest().getConnectedCluster() != null) {
            long start = System.currentTimeMillis();
            Optional<StackInputs> stackInputs = sharedServiceConfigProvider.prepareDatalakeConfigs(stack.getCluster().getBlueprint(), stack);
            if (stackInputs.isPresent()) {
                stack = sharedServiceConfigProvider.updateStackinputs(stackInputs.get(), stack);
            }
            LOGGER.info("Cluster object and its dependencies has been created in {} ms for stack {}", System.currentTimeMillis() - start, stackName);
        }
        return stack;
    }

    private void createClusterIfNeed(User user, Workspace workspace, StackRequest stackRequest, Stack stack, String stackName, Blueprint blueprint)
            throws CloudbreakImageNotFoundException, IOException, TransactionExecutionException {
        if (stackRequest.getClusterRequest() != null) {
            long start = System.currentTimeMillis();
            Cluster cluster = clusterCreationService.prepare(stackRequest.getClusterRequest(), stack, blueprint, user, workspace);
            LOGGER.info("Cluster object and its dependencies has been created in {} ms for stack {}", System.currentTimeMillis() - start, stackName);
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
}
