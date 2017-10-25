package com.sequenceiq.cloudbreak.controller;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.BYOS;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.common.StackEndpoint;
import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson;
import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.cloudbreak.api.model.CertificateResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.StackValidationRequest;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.validation.StackSensitiveDataPropagator;
import com.sequenceiq.cloudbreak.controller.validation.filesystem.FileSystemValidator;
import com.sequenceiq.cloudbreak.controller.validation.stack.StackValidator;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.domain.StackValidation;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesValidationFailed;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesValidator;
import com.sequenceiq.cloudbreak.service.decorator.StackDecorator;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class StackCommonService implements StackEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCommonService.class);

    @Autowired
    private StackService stackService;

    @Autowired
    private TlsSecurityService tlsSecurityService;

    @Autowired
    private StackDecorator stackDecorator;

    @Autowired
    private AccountPreferencesValidator accountPreferencesValidator;

    @Autowired
    private CloudParameterService parameterService;

    @Autowired
    private FileSystemValidator fileSystemValidator;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    private StackValidator stackValidator;

    @Autowired
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Autowired
    private StackSensitiveDataPropagator stackSensitiveDataPropagator;

    @Autowired
    private ClusterCreationSetupService clusterCreationService;

    @Inject
    private StackCreatorService stackCreatorService;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    public StackResponse postPrivate(StackRequest stackRequest) throws Exception {
        return stackCreatorService.createStack(authenticatedUserService.getCbUser(), stackRequest, false);
    }

    public StackResponse postPublic(StackRequest stackRequest) throws Exception {
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
        if (forced) {
            stackService.forceDelete(id, user, deleteDependencies);
        } else {
            stackService.delete(id, user, deleteDependencies);
        }
    }

    @Override
    public void deletePrivate(String name, Boolean forced, Boolean deleteDependencies) {
        IdentityUser user = authenticatedUserService.getCbUser();
        if (forced) {
            stackService.forceDelete(name, user, deleteDependencies);
        } else {
            stackService.delete(name, user, deleteDependencies);
        }
    }

    @Override
    public void deletePublic(String name, Boolean forced, Boolean deleteDependencies) {
        IdentityUser user = authenticatedUserService.getCbUser();
        if (forced) {
            stackService.forceDelete(name, user, deleteDependencies);
        } else {
            stackService.delete(name, user, deleteDependencies);
        }
    }

    public Response put(Long id, UpdateStackJson updateRequest) {
        StackView stack = stackService.getByIdView(id);
        MDCBuilder.buildMdcContext(stack);
        if (updateRequest.getStatus() != null) {
            stackService.updateStatus(id, updateRequest.getStatus(), updateRequest.getWithClusterEvent());
        } else if (!BYOS.equals(stack.cloudPlatform())) {
            Integer scalingAdjustment = updateRequest.getInstanceGroupAdjustment().getScalingAdjustment();
            validateAccountPreferences(id, scalingAdjustment);
            stackService.updateNodeCount(id, updateRequest.getInstanceGroupAdjustment(), updateRequest.getWithClusterEvent());
        } else if (BYOS.equals(stack.cloudPlatform())) {
            return Response.status(Status.BAD_REQUEST).build();
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
        return Response.status(Status.ACCEPTED).build();
    }

    @Override
    public Response deleteInstance(Long stackId, String instanceId) {
        IdentityUser user = authenticatedUserService.getCbUser();
        stackService.removeInstance(user, stackId, instanceId);
        return Response.status(Status.ACCEPTED).build();
    }

    @Override
    public PlatformVariantsJson variants() {
        PlatformVariants pv = parameterService.getPlatformVariants();
        return conversionService.convert(pv, PlatformVariantsJson.class);
    }

    private void validateAccountPreferences(Long stackId, Integer scalingAdjustment) {
        try {
            accountPreferencesValidator.validate(stackId, scalingAdjustment);
        } catch (AccountPreferencesValidationFailed e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }
}
