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
    private StackCommonService stackCommonService;

    @Override
    public StackResponse postPrivate(StackRequest stackRequest) throws Exception {
        return stackCommonService.postPrivate(stackRequest);
    }

    @Override
    public StackResponse postPublic(StackRequest stackRequest) throws Exception {
        return stackCommonService.postPublic(stackRequest);
    }

    @Override
    public Set<StackResponse> getPrivates() {
        return stackCommonService.getPrivates();
    }

    @Override
    public Set<StackResponse> getPublics() {
        return stackCommonService.getPublics();
    }

    @Override
    public StackResponse getPrivate(String name, Set<String> entries) {
        return stackCommonService.getPrivate(name, entries);
    }

    @Override
    public StackResponse getPublic(String name, Set<String> entries) {
        return stackCommonService.getPublic(name, entries);
    }

    @Override
    public StackResponse get(Long id, Set<String> entries) {
        return stackCommonService.get(id, entries);
    }

    @Override
    public void deletePublic(String name, Boolean forced, Boolean deleteDependencies) {
        stackCommonService.deletePublic(name, forced, deleteDependencies);
    }

    @Override
    public void deletePrivate(String name, Boolean forced, Boolean deleteDependencies) {
        stackCommonService.deletePrivate(name, forced, deleteDependencies);
    }

    @Override
    public void delete(Long id, Boolean forced, Boolean deleteDependencies) {
        stackCommonService.delete(id, forced, deleteDependencies);
    }

    @Override
    public Response put(Long id, UpdateStackJson updateRequest) {
        return stackCommonService.put(id, updateRequest);
    }

    @Override
    public Map<String, Object> status(Long id) {
        return stackCommonService.status(id);
    }

    @Override
    public PlatformVariantsJson variants() {
        return stackCommonService.variants();
    }

    @Override
    public Response deleteInstance(Long stackId, String instanceId) {
        return stackCommonService.deleteInstance(stackId, instanceId);
    }

    @Override
    public CertificateResponse getCertificate(Long stackId) {
        return stackCommonService.getCertificate(stackId);
    }

    @Override
    public Response validate(StackValidationRequest stackValidationRequest) {
        return stackCommonService.validate(stackValidationRequest);
    }

    @Override
    public StackResponse getStackForAmbari(AmbariAddressJson json) {
        return stackCommonService.getStackForAmbari(json);
    }

    @Override
    public Set<AutoscaleStackResponse> getAllForAutoscale() {
        return stackCommonService.getAllForAutoscale();
    }
}
