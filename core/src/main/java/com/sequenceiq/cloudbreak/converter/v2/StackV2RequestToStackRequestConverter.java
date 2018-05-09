package com.sequenceiq.cloudbreak.converter.v2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.HostGroupRequest;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.api.model.NetworkRequest;
import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest;
import com.sequenceiq.cloudbreak.api.model.SharedServiceRequest;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateValidator;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class StackV2RequestToStackRequestConverter extends AbstractConversionServiceAwareConverter<StackV2Request, StackRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackV2RequestToStackRequestConverter.class);

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private StackService stackService;

    @Inject
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private TemplateValidator templateValidator;

    @Override
    public StackRequest convert(StackV2Request source) {
        StackRequest stackRequest = new StackRequest();

        stackRequest.setName(source.getGeneral().getName());
        if (source.getPlacement() != null) {
            stackRequest.setAvailabilityZone(source.getPlacement().getAvailabilityZone());
            stackRequest.setRegion(source.getPlacement().getRegion());
        }
        stackRequest.setPlatformVariant(source.getPlatformVariant());
        stackRequest.setAmbariVersion(source.getAmbariVersion());
        stackRequest.setHdpVersion(source.getHdpVersion());
        stackRequest.setParameters(convertParameters(source.getParameters()));
        if (source.getCustomDomain() != null) {
            stackRequest.setCustomDomain(source.getCustomDomain().getCustomDomain());
            stackRequest.setCustomHostname(source.getCustomDomain().getCustomHostname());
            stackRequest.setClusterNameAsSubdomain(source.getCustomDomain().isClusterNameAsSubdomain());
            stackRequest.setHostgroupNameAsHostname(source.getCustomDomain().isHostgroupNameAsHostname());
        }
        if (source.getTags() != null) {
            stackRequest.setApplicationTags(source.getTags().getApplicationTags());
            stackRequest.setDefaultTags(source.getTags().getDefaultTags());
            stackRequest.setUserDefinedTags(source.getTags().getUserDefinedTags());
        }
        stackRequest.setInstanceGroups(new ArrayList<>());
        for (InstanceGroupV2Request instanceGroupV2Request : source.getInstanceGroups()) {
            InstanceGroupRequest convert = conversionService.convert(instanceGroupV2Request, InstanceGroupRequest.class);
            stackRequest.getInstanceGroups().add(convert);
        }
        stackRequest.setFailurePolicy(source.getFailurePolicy());
        stackRequest.setStackAuthentication(source.getStackAuthentication());

        stackRequest.setNetwork(conversionService.convert(source.getNetwork(), NetworkRequest.class));

        OrchestratorRequest orchestrator = new OrchestratorRequest();
        orchestrator.setType("SALT");
        stackRequest.setOrchestrator(orchestrator);
        if (source.getImageSettings() != null) {
            stackRequest.setImageCatalog(source.getImageSettings().getImageCatalog());
            stackRequest.setImageId(source.getImageSettings().getImageId());
        }
        stackRequest.setFlexId(source.getFlexId());
        stackRequest.setCredentialName(source.getGeneral().getCredentialName());
        stackRequest.setOwner(Strings.isNullOrEmpty(source.getOwner()) ? authenticatedUserService.getCbUser().getUserId() : source.getOwner());
        stackRequest.setAccount(Strings.isNullOrEmpty(source.getAccount()) ? authenticatedUserService.getCbUser().getAccount() : source.getAccount());
        stackRequest.setOwnerEmail(Strings.isNullOrEmpty(source.getOwnerEmail()) ? authenticatedUserService.getCbUser().getUsername() : source.getOwnerEmail());
        convertClusterRequest(source, stackRequest);
        stackRequest.setCloudPlatform(credentialService.get(stackRequest.getCredentialName(), stackRequest.getAccount()).cloudPlatform());
        return stackRequest;
    }

    private Map<String, String> convertParameters(Map<String, ?> map) {
        if (map == null) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        for (Entry<String, ?> e : map.entrySet()) {
            result.put(e.getKey(), e.getValue().toString());
        }
        return result;
    }

    private void convertClusterRequest(StackV2Request source, StackRequest stackRequest) {
        if (source.getCluster() != null) {
            stackRequest.setClusterRequest(conversionService.convert(source.getCluster(), ClusterRequest.class));
            for (InstanceGroupV2Request instanceGroupV2Request : source.getInstanceGroups()) {
                HostGroupRequest convert = conversionService.convert(instanceGroupV2Request, HostGroupRequest.class);
                stackRequest.getClusterRequest().getHostGroups().add(convert);
            }
            stackRequest.getClusterRequest().setName(source.getGeneral().getName());
            if (sharedServiceConfigProvider.isConfigured(source.getCluster())) {
                SharedServiceRequest sharedService = source.getCluster().getSharedService();
                stackRequest.setClusterToAttach(stackService.getPublicStack(sharedService.getSharedCluster(), authenticatedUserService.getCbUser()).getId());
            }
        }
    }
}
