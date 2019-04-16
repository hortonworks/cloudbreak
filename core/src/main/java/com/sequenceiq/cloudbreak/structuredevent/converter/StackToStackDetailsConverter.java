package com.sequenceiq.cloudbreak.structuredevent.converter;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;

@Component
public class StackToStackDetailsConverter extends AbstractConversionServiceAwareConverter<Stack, StackDetails> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackToStackDetailsConverter.class);

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public StackDetails convert(Stack source) {
        StackDetails stackDetails = new StackDetails();
        stackDetails.setId(source.getId());
        stackDetails.setName(source.getName());
        stackDetails.setType(source.getType().name());
        stackDetails.setDatalakeResourceId(source.getDatalakeResourceId());
        stackDetails.setRegion(source.getRegion());
        stackDetails.setAvailabilityZone(source.getAvailabilityZone());
        stackDetails.setDescription(source.getDescription());
        stackDetails.setCloudPlatform(source.cloudPlatform());
        stackDetails.setPlatformVariant(source.getPlatformVariant());
        stackDetails.setStatus(source.getStatus().name());
        stackDetails.setDetailedStatus(source.getStackStatus().getDetailedStackStatus().name());
        stackDetails.setStatusReason(source.getStatusReason());
        stackDetails.setInstanceGroups(converterUtil.convertAll(source.getInstanceGroups(), InstanceGroupDetails.class));
        convertComponents(stackDetails, source);
        convertNetwork(stackDetails, source.getNetwork(), source.cloudPlatform());
        return stackDetails;
    }

    private void convertComponents(StackDetails stackDetails, Stack stack) {
        Long stackId = stack.getId();
        CloudbreakDetails cloudbreakDetails = componentConfigProviderService.getCloudbreakDetails(stackId);
        if (cloudbreakDetails != null) {
            stackDetails.setCloudbreakVersion(cloudbreakDetails.getVersion());
        }
        try {
            Image image = componentConfigProviderService.getImage(stackId);
            stackDetails.setImageIdentifier(image.getImageName());
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.warn("Image not found! {}", e.getMessage());
        }
        AmbariRepo ambariRepo = componentConfigProviderService.getAmbariRepo(stackId);
        if (ambariRepo != null) {
            stackDetails.setPrewarmedImage(ambariRepo.getPredefined());
            stackDetails.setAmbariVersion(ambariRepo.getVersion());
        } else {
            stackDetails.setPrewarmedImage(Boolean.FALSE);
        }
        StackRepoDetails stackRepoDetails = componentConfigProviderService.getHDPRepo(stackId);
        if (stackRepoDetails != null) {
            stackDetails.setClusterType(stackRepoDetails.getStack().get(StackRepoDetails.REPO_ID_TAG));
            stackDetails.setClusterVersion(stackRepoDetails.getHdpVersion());
        }
    }

    private void convertNetwork(StackDetails stackDetails, Network network, String cloudPlatform) {
        Boolean existingNetwork = Boolean.FALSE;
        Boolean existingSubnet = Boolean.FALSE;
        if (network != null) {
            Json attributes = network.getAttributes();
            Map<String, Object> params = attributes == null ? Collections.emptyMap() : attributes.getMap();
            switch (cloudPlatform) {
                case CloudConstants.GCP:
                    existingNetwork = StringUtils.isNoneEmpty((CharSequence) params.get("networkId"));
                    existingSubnet = StringUtils.isNoneEmpty((CharSequence) params.get("subnetId"));
                    break;
                case CloudConstants.AWS:
                    existingNetwork = StringUtils.isNoneEmpty((CharSequence) params.get("vpcId"));
                    existingSubnet = StringUtils.isNoneEmpty((CharSequence) params.get("subnetId"));
                    break;
                case CloudConstants.AZURE:
                    existingNetwork = StringUtils.isNoneEmpty((CharSequence) params.get("networkId"))
                            && StringUtils.isNoneEmpty((CharSequence) params.get("resourceGroupName"))
                            && StringUtils.isNoneEmpty((CharSequence) params.get("subnetId"));
                    existingSubnet = Boolean.TRUE;
                    break;
                case CloudConstants.OPENSTACK:
                    existingNetwork = StringUtils.isNoneEmpty((CharSequence) params.get("networkId"));
                    existingSubnet = StringUtils.isNoneEmpty((CharSequence) params.get("subnetId"));
                    break;
                default:
                    existingNetwork = Boolean.FALSE;
                    existingSubnet = Boolean.FALSE;
                    break;
            }
        }
        stackDetails.setExistingNetwork(existingNetwork);
        stackDetails.setExistingSubnet(existingSubnet);
    }
}
