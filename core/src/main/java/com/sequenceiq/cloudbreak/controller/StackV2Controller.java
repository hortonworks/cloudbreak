package com.sequenceiq.cloudbreak.controller;

import com.sequenceiq.cloudbreak.api.endpoint.v2.StackV2Endpoint;
import com.sequenceiq.cloudbreak.api.model.*;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.blueprint.CentralBlueprintUpdater;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.templateprocessor.processor.PreparationObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Set;

@Component
public class StackV2Controller extends NotificationController implements StackV2Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackV2Controller.class);

    @Autowired
    private StackCommonService stackCommonService;

    @Autowired
    private ClusterCommonService clusterCommonController;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    private StackService stackService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private CloudParameterCache cloudParameterCache;

    @Autowired
    private CentralBlueprintUpdater centralBlueprintUpdater;

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
    public Response putScaling(String name, StackScaleRequestV2 updateRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        Stack stack = stackService.getPublicStack(name, user);
        if (!cloudParameterCache.isScalingSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Scaling is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        updateRequest.setStackId(stack.getId());
        UpdateStackJson updateStackJson = conversionService.convert(updateRequest, UpdateStackJson.class);
        if (updateStackJson.getInstanceGroupAdjustment().getScalingAdjustment() > 0) {
            return stackCommonService.put(stack.getId(), updateStackJson);
        } else {
            UpdateClusterJson updateClusterJson = conversionService.convert(updateRequest, UpdateClusterJson.class);
            return clusterCommonController.put(stack.getId(), updateClusterJson);
        }
    }

    @Override
    public Response putStart(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        Stack stack = stackService.getPublicStack(name, user);
        if (!cloudParameterCache.isStartStopSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Start is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setStatus(StatusRequest.STARTED);
        updateStackJson.setWithClusterEvent(true);
        return stackCommonService.put(stack.getId(), updateStackJson);
    }

    @Override
    public Response putStop(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        Stack stack = stackService.getPublicStack(name, user);
        if (!cloudParameterCache.isStartStopSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Stop is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setStatus(StatusRequest.STOPPED);
        updateStackJson.setWithClusterEvent(true);
        return stackCommonService.put(stack.getId(), updateStackJson);
    }

    @Override
    public Response putSync(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        Stack stack = stackService.getPublicStack(name, user);
        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setStatus(StatusRequest.FULL_SYNC);
        updateStackJson.setWithClusterEvent(true);
        return stackCommonService.put(stack.getId(), updateStackJson);
    }

    @Override
    public Response putRepair(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        Stack stack = stackService.getPublicStack(name, user);
        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setStatus(StatusRequest.REPAIR_FAILED_NODES);
        updateStackJson.setWithClusterEvent(true);
        return stackCommonService.put(stack.getId(), updateStackJson);
    }

    @Override
    public Response putReinstall(String name, ReinstallRequestV2 reinstallRequestV2) {
        IdentityUser user = authenticatedUserService.getCbUser();
        reinstallRequestV2.setAccount(user.getAccount());
        Stack stack = stackService.getPublicStack(name, user);
        UpdateClusterJson updateClusterJson = conversionService.convert(reinstallRequestV2, UpdateClusterJson.class);
        return clusterCommonController.put(stack.getId(), updateClusterJson);
    }

    @Override
    public Response putPassword(String name, UserNamePasswordJson userNamePasswordJson) {
        IdentityUser user = authenticatedUserService.getCbUser();
        Stack stack = stackService.getPublicStack(name, user);
        UpdateClusterJson updateClusterJson = conversionService.convert(userNamePasswordJson, UpdateClusterJson.class);
        return clusterCommonController.put(stack.getId(), updateClusterJson);
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

    @Override
    public StackV2Request getRequestfromName(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return stackService.getStackRequestByName(name, user);
    }

    @Override
    public StackResponse postPrivate(StackV2Request stackRequest) throws Exception {
        return stackCommonService.postPrivate(conversionService.convert(stackRequest, StackRequest.class));
    }

    @Override
    public StackResponse postPublic(StackV2Request stackRequest) throws Exception {
        return stackCommonService.postPublic(conversionService.convert(stackRequest, StackRequest.class));
    }

    @Override
    public GeneratedBlueprintResponse postStackForBlueprint(StackV2Request stackRequest) throws Exception {
        IdentityUser user = authenticatedUserService.getCbUser();
        stackRequest.setAccount(user.getAccount());
        stackRequest.setOwner(user.getUserId());
        PreparationObject blueprintPreparationObject = conversionService.convert(stackRequest, PreparationObject.class);
        String blueprintText = centralBlueprintUpdater.getBlueprintText(blueprintPreparationObject);
        return new GeneratedBlueprintResponse(blueprintText);
    }
}
