package com.sequenceiq.cloudbreak.controller;

import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v2.StackV2Endpoint;
import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson;
import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.cloudbreak.api.model.CertificateResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.StackValidationRequest;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
import com.sequenceiq.cloudbreak.api.model.UpdateStackRequestV2;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class StackV2Controller extends NotificationController implements StackV2Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackV2Controller.class);

    @Autowired
    private StackCommonController stackCommonController;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    private StackService stackService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public Set<StackResponse> getPrivates() {
        return stackCommonController.getPrivates();
    }

    @Override
    public Set<StackResponse> getPublics() {
        return stackCommonController.getPublics();
    }

    @Override
    public StackResponse getPrivate(String name, Set<String> entries) {
        return stackCommonController.getPrivate(name, entries);
    }

    @Override
    public StackResponse getPublic(String name, Set<String> entries) {
        return stackCommonController.getPublic(name, entries);
    }

    @Override
    public StackResponse get(Long id, Set<String> entries) {
        return stackCommonController.get(id, entries);
    }

    @Override
    public void deletePublic(String name, Boolean forced, Boolean deleteDependencies) {
        stackCommonController.deletePublic(name, forced, deleteDependencies);
    }

    @Override
    public void deletePrivate(String name, Boolean forced, Boolean deleteDependencies) {
        stackCommonController.deletePrivate(name, forced, deleteDependencies);
    }

    @Override
    public void delete(Long id, Boolean forced, Boolean deleteDependencies) {
        stackCommonController.delete(id, forced, deleteDependencies);
    }

    public Response put(String name, UpdateStackRequestV2 updateRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        Stack stack = stackService.getPublicStack(name, user);
        updateRequest.setStackId(stack.getId());
        UpdateStackJson updateStackJson = conversionService.convert(updateRequest, UpdateStackJson.class);
        return stackCommonController.put(stack.getId(), updateStackJson);
    }

    @Override
    public Map<String, Object> status(Long id) {
        return stackCommonController.status(id);
    }

    @Override
    public PlatformVariantsJson variants() {
        return stackCommonController.variants();
    }

    @Override
    public Response deleteInstance(Long stackId, String instanceId) {
        return stackCommonController.deleteInstance(stackId, instanceId);
    }

    @Override
    public CertificateResponse getCertificate(Long stackId) {
        return stackCommonController.getCertificate(stackId);
    }

    @Override
    public Response validate(StackValidationRequest stackValidationRequest) {
        return stackCommonController.validate(stackValidationRequest);
    }

    @Override
    public StackResponse getStackForAmbari(AmbariAddressJson json) {
        return stackCommonController.getStackForAmbari(json);
    }

    @Override
    public Set<AutoscaleStackResponse> getAllForAutoscale() {
        return stackCommonController.getAllForAutoscale();
    }

    @Override
    public StackResponse postPrivate(StackV2Request stackRequest) throws Exception {
        return stackCommonController.postPrivate(conversionService.convert(stackRequest, StackRequest.class));
    }

    @Override
    public StackResponse postPublic(StackV2Request stackRequest) throws Exception {
        return stackCommonController.postPublic(conversionService.convert(stackRequest, StackRequest.class));
    }
}
