package com.sequenceiq.cloudbreak.controller;

import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint;
import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson;
import com.sequenceiq.cloudbreak.api.model.CertificateResponse;
import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.StackValidationRequest;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.controller.validation.filesystem.FileSystemValidator;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackValidation;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesValidationFailed;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesValidator;
import com.sequenceiq.cloudbreak.service.decorator.Decorator;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class StackController implements StackEndpoint {

    @Autowired
    private StackService stackService;

    @Autowired
    private TlsSecurityService tlsSecurityService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private Decorator<Stack> stackDecorator;

    @Autowired
    private AccountPreferencesValidator accountPreferencesValidator;

    @Autowired
    private CloudParameterService parameterService;

    @Autowired
    private FileSystemValidator fileSystemValidator;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Override
    public IdJson postPrivate(StackRequest stackRequest) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        return createStack(user, stackRequest, false);
    }

    @Override
    public IdJson postPublic(StackRequest stackRequest) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        return createStack(user, stackRequest, true);
    }

    @Override
    public Set<StackResponse> getPrivates() {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        return stackService.retrievePrivateStacks(user);
    }

    @Override
    public Set<StackResponse> getPublics() {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        return stackService.retrieveAccountStacks(user);
    }

    @Override
    public StackResponse get(Long id) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        return stackService.getJsonById(id);
    }

    @Override
    public StackResponse getPrivate(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        return stackService.getPrivateStackJsonByName(name, user);
    }

    @Override
    public StackResponse getPublic(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        return stackService.getPublicStackJsonByName(name, user);
    }

    @Override
    public Map<String, Object> status(Long id) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        return conversionService.convert(stackService.get(id), Map.class);
    }

    @Override
    public void delete(Long id, Boolean forced) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        if (forced) {
            stackService.forceDelete(id, user);
        } else {
            stackService.delete(id, user);
        }
    }

    @Override
    public void deletePrivate(String name, Boolean forced) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        if (forced) {
            stackService.forceDelete(name, user);
        } else {
            stackService.delete(name, user);
        }
    }

    @Override
    public void deletePublic(String name, Boolean forced) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        if (forced) {
            stackService.forceDelete(name, user);
        } else {
            stackService.delete(name, user);
        }
    }

    @Override
    public Response put(Long id, UpdateStackJson updateRequest) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        if (updateRequest.getStatus() != null) {
            stackService.updateStatus(id, updateRequest.getStatus());
            return Response.status(Response.Status.ACCEPTED).build();
        } else {
            Integer scalingAdjustment = updateRequest.getInstanceGroupAdjustment().getScalingAdjustment();
            validateAccountPreferences(id, scalingAdjustment);
            stackService.updateNodeCount(id, updateRequest.getInstanceGroupAdjustment());
            return Response.status(Response.Status.ACCEPTED).build();
        }
    }

    @Override
    public CertificateResponse getCertificate(Long stackId) {
        return new CertificateResponse(tlsSecurityService.getCertificate(stackId));
    }

    @Override
    public StackResponse getStackForAmbari(AmbariAddressJson json) {
        return stackService.get(json.getAmbariAddress());
    }

    @Override
    public Response validate(StackValidationRequest request) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        StackValidation stackValidation = conversionService.convert(request, StackValidation.class);
        stackService.validateStack(stackValidation);
        fileSystemValidator.validateFileSystem(request.getPlatform(), request.getFileSystem());
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @Override
    public Response deleteInstance(Long stackId, String instanceId) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildMdcContext(user);
        stackService.removeInstance(user, stackId, instanceId);
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @Override
    public PlatformVariantsJson variants() {
        PlatformVariants pv = parameterService.getPlatformVariants();
        return conversionService.convert(pv, PlatformVariantsJson.class);
    }

    private IdJson createStack(CbUser user, StackRequest stackRequest, boolean publicInAccount) {
        Stack stack = conversionService.convert(stackRequest, Stack.class);
        MDCBuilder.buildMdcContext(stack);
        stack = stackDecorator.decorate(stack, stackRequest.getCredentialId(), stackRequest.getConsulServerCount(), stackRequest.getNetworkId(),
                stackRequest.getSecurityGroupId());
        stack.setPublicInAccount(publicInAccount);
        validateAccountPreferences(stack, user);
        stackService.validateOrchestrator(stack.getOrchestrator());
        stack = stackService.create(user, stack);
        return new IdJson(stack.getId());
    }

    private void validateAccountPreferences(Stack stack, CbUser user) {
        try {
            accountPreferencesValidator.validate(stack, user.getAccount(), user.getUserId());
        } catch (AccountPreferencesValidationFailed e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    private void validateAccountPreferences(Long stackId, Integer scalingAdjustment) {
        try {
            accountPreferencesValidator.validate(stackId, scalingAdjustment);
        } catch (AccountPreferencesValidationFailed e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }
}
