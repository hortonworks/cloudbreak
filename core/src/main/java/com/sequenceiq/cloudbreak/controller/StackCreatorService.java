package com.sequenceiq.cloudbreak.controller;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.StackValidationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.validation.StackSensitiveDataPropagator;
import com.sequenceiq.cloudbreak.controller.validation.filesystem.FileSystemValidator;
import com.sequenceiq.cloudbreak.controller.validation.stack.StackValidator;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackValidation;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesValidationFailed;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesValidator;
import com.sequenceiq.cloudbreak.service.decorator.StackDecorator;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class StackCreatorService {

    @Inject
    private StackDecorator stackDecorator;

    @Inject
    private FileSystemValidator fileSystemValidator;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private StackValidator stackValidator;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private StackSensitiveDataPropagator stackSensitiveDataPropagator;

    @Inject
    private ClusterCreationSetupService clusterCreationService;

    @Inject
    private StackService stackService;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private AccountPreferencesValidator accountPreferencesValidator;

    public StackResponse createStack(IdentityUser user, StackV2Request stackV2Request, boolean publicInAccount) throws Exception {
        StackRequest stackRequest = conversionService.convert(stackV2Request, StackRequest.class);
        return createStack(user, stackRequest, publicInAccount);
    }

    public StackResponse createStack(IdentityUser user, StackRequest stackRequest, boolean publicInAccount) throws Exception {
        stackRequest.setAccount(user.getAccount());
        stackRequest.setOwner(user.getUserId());
        stackValidator.validate(user, stackRequest.getName(), stackRequest.getCredentialSource(),
                stackRequest.getCredentialId(), stackRequest.getCredential(), stackRequest.getParameters());
        Stack stack = conversionService.convert(stackRequest, Stack.class);
        MDCBuilder.buildMdcContext(stack);
        stack = stackSensitiveDataPropagator.propagate(stackRequest.getCredentialSource(), stack, user);
        stack = stackDecorator.decorate(stack, stackRequest, user);
        stack.setPublicInAccount(publicInAccount);
        validateAccountPreferences(stack, user);

        if (stack.getOrchestrator() != null && stack.getOrchestrator().getApiEndpoint() != null) {
            stackService.validateOrchestrator(stack.getOrchestrator());
        }

        if (stackRequest.getClusterRequest() != null) {
            StackValidationRequest stackValidationRequest = conversionService.convert(stackRequest, StackValidationRequest.class);
            StackValidation stackValidation = conversionService.convert(stackValidationRequest, StackValidation.class);
            stackService.validateStack(stackValidation, stackRequest.getClusterRequest().getValidateBlueprint());
            CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(stackValidation.getCredential());
            fileSystemValidator.validateFileSystem(stackValidationRequest.getPlatform(), cloudCredential, stackValidationRequest.getFileSystem());
            clusterCreationService.validate(stackRequest.getClusterRequest(), stack, user);
        }

        stack = stackService.create(user, stack, stackRequest.getAmbariVersion(), stackRequest.getHdpVersion(),
                stackRequest.getImageCatalog(), Optional.ofNullable(stackRequest.getCustomImage()));

        if (stackRequest.getClusterRequest() != null) {
            Cluster cluster = clusterCreationService.prepare(stackRequest.getClusterRequest(), stack, user);
            stack.setCluster(cluster);
        }
        return conversionService.convert(stack, StackResponse.class);
    }

    private void validateAccountPreferences(Stack stack, IdentityUser user) {
        try {
            accountPreferencesValidator.validate(stack, user.getAccount(), user.getUserId());
        } catch (AccountPreferencesValidationFailed e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }
}
