package com.sequenceiq.cloudbreak.structuredevent.converter;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.HDPRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;

@Component
public class StackToStackDetailsConverter extends AbstractConversionServiceAwareConverter<Stack, StackDetails> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintToBlueprintDetailsConverter.class);

    @Inject
    private ConversionService conversionService;

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    @Override
    public StackDetails convert(Stack source) {
        StackDetails stackDetails = new StackDetails();
        stackDetails.setStackId(source.getId());
        stackDetails.setStackName(source.getName());
        stackDetails.setRegion(source.getRegion());
        stackDetails.setAvailabilityZone(source.getAvailabilityZone());
        stackDetails.setDescription(source.getDescription());
        stackDetails.setCloudPlatform(source.cloudPlatform());
        stackDetails.setPlatformVariant(source.getPlatformVariant());
        stackDetails.setStackStatus(source.getStatus().name());
        stackDetails.setDetailedStackStatus(source.getStackStatus().getDetailedStackStatus().name());
        stackDetails.setStatusReason(source.getStatusReason());
        stackDetails.setInstanceGroups((Set<InstanceGroupDetails>) conversionService.convert(source.getInstanceGroups(),
                TypeDescriptor.forObject(source.getInstanceGroups()),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(InstanceGroupDetails.class))));
        convertComponents(stackDetails, source);
        convertNetwork(stackDetails, source.getNetwork(), source.cloudPlatform());
        return stackDetails;
    }

    private void convertComponents(StackDetails stackDetails, Stack stack) {
        Long stackId = stack.getId();
        CloudbreakDetails cloudbreakDetails = componentConfigProvider.getCloudbreakDetails(stackId);
        if (cloudbreakDetails != null) {
            stackDetails.setCloudbreakVersion(cloudbreakDetails.getVersion());
        }
        try {
            Image image = componentConfigProvider.getImage(stackId);
            stackDetails.setImageIdentifier(image.getImageName());
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.warn("Image not found!", e.getMessage());
        }
        AmbariRepo ambariRepo = componentConfigProvider.getAmbariRepo(stackId);
        if (ambariRepo != null) {
            stackDetails.setPrewarmedImage(ambariRepo.getPredefined());
            stackDetails.setAmbariVersion(ambariRepo.getVersion());
        } else {
            stackDetails.setPrewarmedImage(Boolean.FALSE);
        }
        HDPRepo hdpRepo = componentConfigProvider.getHDPRepo(stackId);
        if (hdpRepo != null) {
            stackDetails.setHdpVersion(hdpRepo.getHdpVersion());
        }
    }

    private void convertNetwork(StackDetails stackDetails, Network network, String cloudPlatform) {
        if (network != null) {
            Boolean existingNetwork;
            Boolean existingSubnet;
            Json attributes = network.getAttributes();
            Map<String, Object> params = attributes == null ? Collections.emptyMap() : attributes.getMap();
            switch (cloudPlatform) {
                case CloudConstants.GCP:
                    existingNetwork = StringUtils.isNoneEmpty((String) params.get("networkId"));
                    existingSubnet = StringUtils.isNoneEmpty((String) params.get("subnetId"));
                    break;
                case CloudConstants.AWS:
                    existingNetwork = StringUtils.isNoneEmpty((String) params.get("vpcId"));
                    existingSubnet = StringUtils.isNoneEmpty((String) params.get("subnetId"));
                    break;
                case CloudConstants.AZURE:
                    existingNetwork = StringUtils.isNoneEmpty((String) params.get("networkId"))
                            && StringUtils.isNoneEmpty((String) params.get("resourceGroupName")) && StringUtils.isNoneEmpty((String) params.get("subnetId"));
                    existingSubnet = Boolean.TRUE;
                    break;
                case CloudConstants.OPENSTACK:
                    existingNetwork = StringUtils.isNoneEmpty((String) params.get("networkId"));
                    existingSubnet = StringUtils.isNoneEmpty((String) params.get("subnetId"));
                    break;
                default:
                    existingNetwork = Boolean.FALSE;
                    existingSubnet = Boolean.TRUE;
                    break;
            }
            stackDetails.setExistingNetwork(existingNetwork);
            stackDetails.setExisitngSubnet(existingSubnet);
        }
    }
}
