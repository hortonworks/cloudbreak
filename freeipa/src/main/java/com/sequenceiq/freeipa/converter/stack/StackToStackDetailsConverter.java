package com.sequenceiq.freeipa.converter.stack;


import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.FreeIpaStackDetails;
import com.sequenceiq.freeipa.converter.image.ImageEntityToImageDetailsConverter;
import com.sequenceiq.freeipa.converter.instance.InstanceGroupToInstanceGroupDetailsConverter;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupService;

@Component
public class StackToStackDetailsConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackToStackDetailsConverter.class);

    @Inject
    private ImageService imageService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @Inject
    private InstanceGroupToInstanceGroupDetailsConverter instanceGroupToInstanceGroupDetailsConverter;

    @Inject
    private ImageEntityToImageDetailsConverter imageEntityToImageDetailsConverter;

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    public FreeIpaStackDetails convert(Stack source) {
        FreeIpaStackDetails stackDetails = new FreeIpaStackDetails();
        stackDetails.setId(source.getId());
        stackDetails.setName(source.getName());
        stackDetails.setTunnel(source.getTunnel().name());
        stackDetails.setRegion(source.getRegion());
        stackDetails.setAvailabilityZone(source.getAvailabilityZone());
        stackDetails.setCloudPlatform(source.getCloudPlatform());
        stackDetails.setPlatformVariant(source.getPlatformvariant());
        StackStatus stackStatus = source.getStackStatus();
        if (stackStatus != null) {
            stackDetails.setStatus(stackStatus.getStatusString());
            stackDetails.setDetailedStatus(stackStatus.getDetailedStackStatusString());
            stackDetails.setStatusReason(stackStatus.getStatusReason());
        }
        Set<InstanceGroup> instanceGroups = instanceGroupService.findByStackId(source.getId());
        stackDetails.setMultiAz(getMultiAz(instanceGroups));
        stackDetails.setInstanceGroups(instanceGroups.stream()
                .map(e -> instanceGroupToInstanceGroupDetailsConverter.convert(e))
                .collect(Collectors.toList()));
        stackDetails.setTags(source.getTags());
        convertImage(stackDetails, source);
        stackDetails.setLoadBalancerType(getLoadBalancerType(source.getId()));
        stackDetails.setTrustRelationshipType(convertTrustRelationshipType(source.getId()));
        return stackDetails;
    }

    private String convertTrustRelationshipType(Long id) {
        Optional<CrossRealmTrust> trust = crossRealmTrustService.getByStackIdIfExists(id);
        return trust.map(CrossRealmTrust::getTrustRelationshipType).map(Enum::name).orElse(null);
    }

    private String getLoadBalancerType(Long stackId) {
        return freeIpaLoadBalancerService.findByStackId(stackId).isPresent() ? "ENABLED" : "DISABLED";
    }

    private boolean getMultiAz(Set<InstanceGroup> instanceGroups) {
        return instanceGroups.stream()
                .flatMap(instanceGroup -> getSubnetIds(instanceGroup).stream())
                .distinct()
                .count() > 1;
    }

    private List<String> getSubnetIds(InstanceGroup instanceGroup) {
        Map<String, Object> attributes = Optional.ofNullable(instanceGroup)
                .map(InstanceGroup::getInstanceGroupNetwork)
                .map(InstanceGroupNetwork::getAttributes)
                .map(Json::getMap)
                .orElse(Map.of());
        return (List<String>) attributes.getOrDefault(NetworkConstants.SUBNET_IDS, List.of());
    }

    private void convertImage(StackDetails stackDetails, Stack stack) {
        Long stackId = stack.getId();
        try {
            ImageEntity image = imageService.getByStackId(stackId);
            stackDetails.setImage(imageEntityToImageDetailsConverter.convert(image));
        } catch (Exception e) {
            LOGGER.warn("Image not found! {}", e.getMessage());
        }
    }
}
