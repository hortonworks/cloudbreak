package com.sequenceiq.cloudbreak.service;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.common.StackEndpoint;
import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson;
import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.cloudbreak.api.model.CertificateResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackValidationRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ScalingHardLimitsService;
import com.sequenceiq.cloudbreak.controller.StackCreatorService;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.filesystem.FileSystemValidator;
import com.sequenceiq.cloudbreak.controller.validation.stack.StackRequestValidator;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.stack.StackValidation;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesValidationException;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesValidator;
import com.sequenceiq.cloudbreak.service.decorator.StackDecorator;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class StackCommonService implements StackEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCommonService.class);

    @Inject
    private StackService stackService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private StackDecorator stackDecorator;

    @Inject
    private AccountPreferencesValidator accountPreferencesValidator;

    @Inject
    private CloudParameterService parameterService;

    @Inject
    private FileSystemValidator fileSystemValidator;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private StackRequestValidator stackValidator;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private ClusterCreationSetupService clusterCreationService;

    @Inject
    private StackCreatorService stackCreatorService;

    @Inject
    private ScalingHardLimitsService scalingHardLimitsService;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    public StackResponse postPrivate(StackRequest stackRequest) {
        return stackCreatorService.createStack(authenticatedUserService.getCbUser(), stackRequest, false);
    }

    public StackResponse postPublic(StackRequest stackRequest) {
        return stackCreatorService.createStack(authenticatedUserService.getCbUser(), stackRequest, true);
    }

    @Override
    public Set<StackResponse> getPrivates() {
        IdentityUser user = authenticatedUserService.getCbUser();
        return stackService.retrievePrivateStacks(user);
    }

    @Override
    public Set<StackResponse> getPublics() {
        IdentityUser user = authenticatedUserService.getCbUser();
        return stackService.retrieveAccountStacks(user);
    }

    @Override
    public StackResponse get(Long id, Set<String> entries) {
        return stackService.getJsonById(id, entries);
    }

    @Override
    public StackResponse getPrivate(String name, Set<String> entries) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return stackService.getPrivateStackJsonByName(name, user, entries);
    }

    @Override
    public StackResponse getPublic(String name, Set<String> entries) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return stackService.getPublicStackJsonByName(name, user, entries);
    }

    @Override
    public Map<String, Object> status(Long id) {
        return conversionService.convert(stackService.get(id), Map.class);
    }

    @Override
    public void delete(Long id, Boolean forced, Boolean deleteDependencies) {
        IdentityUser user = authenticatedUserService.getCbUser();
        stackService.delete(id, user, forced, deleteDependencies);
    }

    @Override
    public void deletePrivate(String name, Boolean forced, Boolean deleteDependencies) {
        IdentityUser user = authenticatedUserService.getCbUser();
        stackService.delete(name, user, forced, deleteDependencies);
    }

    @Override
    public void deletePublic(String name, Boolean forced, Boolean deleteDependencies) {
        IdentityUser user = authenticatedUserService.getCbUser();
        stackService.delete(name, user, forced, deleteDependencies);
    }

    public Response put(Long id, UpdateStackJson updateRequest) {
        StackView stack = stackService.getByIdView(id);
        MDCBuilder.buildMdcContext(stack);
        if (updateRequest.getStatus() != null) {
            stackService.updateStatus(id, updateRequest.getStatus(), updateRequest.getWithClusterEvent());
        } else {
            Integer scalingAdjustment = updateRequest.getInstanceGroupAdjustment().getScalingAdjustment();
            validateHardLimits(scalingAdjustment);
            validateAccountPreferences(id, scalingAdjustment);
            stackService.updateNodeCount(id, updateRequest.getInstanceGroupAdjustment(), updateRequest.getWithClusterEvent());
        }
        return Response.status(Status.NO_CONTENT).build();
    }

    @Override
    public CertificateResponse getCertificate(Long stackId) {
        return tlsSecurityService.getCertificates(stackId);
    }

    @Override
    public StackResponse getStackForAmbari(AmbariAddressJson json) {
        return stackService.get(json.getAmbariAddress());
    }

    @Override
    public Set<AutoscaleStackResponse> getAllForAutoscale() {
        LOGGER.info("Get all stack, autoscale authorized only.");
        return stackService.getAllForAutoscale();
    }

    @Override
    public Response validate(StackValidationRequest request) {
        StackValidation stackValidation = conversionService.convert(request, StackValidation.class);
        stackService.validateStack(stackValidation, true);
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(stackValidation.getCredential());
        fileSystemValidator.validateFileSystem(request.getPlatform(), cloudCredential, request.getFileSystem());
        return Response.status(Status.NO_CONTENT).build();
    }

    @Override
    public Response deleteInstance(Long stackId, String instanceId) {
        return deleteInstance(stackId, instanceId, false);
    }

    @Override
    public Response deleteInstance(Long stackId, String instanceId, boolean forced) {
        IdentityUser user = authenticatedUserService.getCbUser();
        stackService.removeInstance(user, stackId, instanceId, forced);
        return Response.status(Status.NO_CONTENT).build();
    }

    @Override
    public Response deleteInstances(Long stackId, Set<String> instanceIds) {
        IdentityUser user = authenticatedUserService.getCbUser();
        stackService.removeInstances(user, stackId, instanceIds);
        return Response.status(Status.NO_CONTENT).build();
    }

    @Override
    public PlatformVariantsJson variants() {
        PlatformVariants pv = parameterService.getPlatformVariants();
        return conversionService.convert(pv, PlatformVariantsJson.class);
    }

    private void validateAccountPreferences(Long stackId, Integer scalingAdjustment) {
        try {
            accountPreferencesValidator.validate(stackId, scalingAdjustment);
        } catch (AccountPreferencesValidationException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    private void validateHardLimits(Integer scalingAdjustment) {
        if (scalingHardLimitsService.isViolatingMaxUpscaleStepInNodeCount(scalingAdjustment)) {
            throw new BadRequestException(String.format("Upscaling by more than %d nodes is not supported",
                    scalingHardLimitsService.getMaxUpscaleStepInNodeCount()));
        }
    }
}
