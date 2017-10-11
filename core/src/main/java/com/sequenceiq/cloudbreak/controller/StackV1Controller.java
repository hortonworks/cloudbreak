package com.sequenceiq.cloudbreak.controller;

import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.StackV1Endpoint;
import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson;
import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.cloudbreak.api.model.CertificateResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.StackValidationRequest;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;

@Component
public class StackV1Controller extends NotificationController implements StackV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackV1Controller.class);

    @Autowired
    private StackCreatorService stackCreatorService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    private StackCommonController stackCommonController;

    @Override
    public StackResponse postPrivate(StackRequest stackRequest) throws Exception {
        return stackCommonController.postPrivate(stackRequest);
    }

    @Override
    public StackResponse postPublic(StackRequest stackRequest) throws Exception {
        return stackCommonController.postPublic(stackRequest);
    }

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

    @Override
    public Response put(Long id, UpdateStackJson updateRequest) {
        return stackCommonController.put(id, updateRequest);
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
}
