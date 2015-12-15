package com.sequenceiq.cloudbreak.controller;

import javax.ws.rs.core.Response;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.StackEndpoint;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackValidation;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.model.AmbariAddressJson;
import com.sequenceiq.cloudbreak.model.IdJson;
import com.sequenceiq.cloudbreak.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.model.StackRequest;
import com.sequenceiq.cloudbreak.model.StackResponse;
import com.sequenceiq.cloudbreak.model.StackValidationRequest;
import com.sequenceiq.cloudbreak.model.UpdateStackJson;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesValidationFailed;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesValidator;
import com.sequenceiq.cloudbreak.service.decorator.Decorator;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

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
        Set<Stack> stacks = stackService.retrievePrivateStacks(user);
        return convertStacks(stacks);
    }

    protected Set<StackResponse> convertStacks(Set<Stack> stacks) {
        return (Set<StackResponse>) conversionService.convert(stacks, TypeDescriptor.forObject(stacks),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(StackResponse.class)));
    }

    @Override
    public Set<StackResponse> getPublics() {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        Set<Stack> stacks = stackService.retrieveAccountStacks(user);
        return convertStacks(stacks);
    }

    @Override
    public StackResponse get(Long id) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        Stack stack = stackService.get(id);
        return conversionService.convert(stack, StackResponse.class);
    }

    @Override
    public StackResponse getPrivate(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        Stack stack = stackService.getPrivateStack(name, user);
        return conversionService.convert(stack, StackResponse.class);
    }

    @Override
    public StackResponse getPublic(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        Stack stack = stackService.getPublicStack(name, user);
        return conversionService.convert(stack, StackResponse.class);
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
    public StackResponse getStackForAmbari(AmbariAddressJson json) {
        Stack stack = stackService.get(json.getAmbariAddress());
        return conversionService.convert(stack, StackResponse.class);
    }

    @Override
    public Response validate(StackValidationRequest stackValidationRequest) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        StackValidation stackValidation = conversionService.convert(stackValidationRequest, StackValidation.class);
        stackService.validateStack(stackValidation);
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
