package com.sequenceiq.cloudbreak.cloud.template.context;

import static com.sequenceiq.cloudbreak.cloud.model.CloudResource.PRIVATE_ID;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;

@Service
public class VolumeMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(VolumeMatcher.class);

    public ResourceBuilderContext addVolumeResourcesToContext(List<CloudInstance> instances, List<CloudResource> groupInstances,
            List<CloudResource> groupVolumeSets, ResourceBuilderContext context) {
        List<CloudResource> groupVolumeSetsWithFQDN = getGroupVolumeSetsWithFQDN(groupVolumeSets);
        List<CloudResource> groupVolumeSetsWithoutFQDN = getGroupVolumeSetsWithoutFQDN(groupVolumeSets);
        List<CloudResource> groupInstancesWithoutPrivateId = groupInstances.stream()
                .filter(groupInstance -> groupInstance.getParameter(PRIVATE_ID, Long.class) == null)
                .collect(Collectors.toList());
        for (int i = 0; i < instances.size() && i < groupInstances.size(); i++) {
            CloudInstance cloudInstance = instances.get(i);
            LOGGER.info("Check volumes for instance: {}", cloudInstance);
            CloudResource instanceResource = getCloudResource(groupInstances, groupInstancesWithoutPrivateId, cloudInstance);
            LOGGER.info("Instance resource: {}", instanceResource);
            Optional<CloudResource> volumeSetForFQDN = getVolumeSetForFQDN(groupVolumeSetsWithFQDN, cloudInstance);
            volumeSetForFQDN.ifPresent(cloudResource -> LOGGER.info("Volume set for this fqdn: {}", cloudResource));
            List<CloudResource> computeResource = volumeSetForFQDN.map(groupVolumeSet -> List.of(instanceResource, groupVolumeSet))
                        .orElseGet(() -> getComputeResourceFromVolumesWithoutFQDN(groupVolumeSetsWithoutFQDN, instanceResource));
            LOGGER.info("Add volume resources {} to instance with private id: {}", computeResource, cloudInstance.getTemplate().getPrivateId());
            context.addComputeResources(cloudInstance.getTemplate().getPrivateId(), computeResource);
        }
        return context;
    }

    private CloudResource getCloudResource(List<CloudResource> groupInstances, List<CloudResource> groupInstancesWithoutPrivateId, CloudInstance cloudInstance) {
        Optional<CloudResource> instanceResourceByPrivateId = getInstanceByPrivateId(groupInstances, cloudInstance);
        return instanceResourceByPrivateId.or(() -> {
            if (groupInstancesWithoutPrivateId.size() > 0) {
                return Optional.of(groupInstancesWithoutPrivateId.remove(0));
            } else {
                return Optional.empty();
            }
        }).orElseThrow(() -> new CloudConnectorException("Can't find cloud instance by private ID: " + cloudInstance.getTemplate().getPrivateId()));
    }

    private Optional<CloudResource> getInstanceByPrivateId(List<CloudResource> groupInstances, CloudInstance cloudInstance) {
        return groupInstances.stream()
                .filter(groupInstance -> cloudInstance.getTemplate().getPrivateId().equals(groupInstance.getParameter(PRIVATE_ID, Long.class)))
                .findFirst();
    }

    private Optional<CloudResource> getVolumeSetForFQDN(List<CloudResource> groupVolumeSetsWithFQDN, CloudInstance cloudInstance) {
        String fqdn = cloudInstance.getParameter(CloudInstance.FQDN, String.class);
        LOGGER.info("FQDN for {}: {} ", cloudInstance, fqdn);
        if (StringUtils.isNotBlank(fqdn)) {
            Optional<CloudResource> volumeSetForFQDN = groupVolumeSetsWithFQDN.stream()
                    .filter(groupVolumeSet -> fqdn.equals(groupVolumeSet.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class).getDiscoveryFQDN()))
                    .findFirst();
            volumeSetForFQDN.ifPresentOrElse(cloudResource -> LOGGER.info("Volume set for this fqdn({}): {}", fqdn, cloudResource),
                    () -> LOGGER.info("No volume set found for this fqdn: {}", fqdn));
            return volumeSetForFQDN;
        } else {
            return Optional.empty();
        }
    }

    private List<CloudResource> getGroupVolumeSetsWithFQDN(List<CloudResource> groupVolumeSets) {
        List<CloudResource> groupVolumeSetsWithFQDN = groupVolumeSets.stream()
                .filter(groupVolumeSet -> groupVolumeSet.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class).getDiscoveryFQDN() != null)
                .collect(Collectors.toList());
        LOGGER.info("Volume sets with FQDN: " + groupVolumeSetsWithFQDN);
        return groupVolumeSetsWithFQDN;
    }

    private List<CloudResource> getGroupVolumeSetsWithoutFQDN(List<CloudResource> groupVolumeSets) {
        List<CloudResource> groupVolumeSetsWithoutFQDN = groupVolumeSets.stream()
                .filter(groupVolumeSet -> groupVolumeSet.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class).getDiscoveryFQDN() == null)
                .collect(Collectors.toList());
        LOGGER.info("Volume sets without FQDN: " + groupVolumeSetsWithoutFQDN);
        return groupVolumeSetsWithoutFQDN;
    }

    private List<CloudResource> getComputeResourceFromVolumesWithoutFQDN(List<CloudResource> groupVolumeSetsWithoutFQDN, CloudResource instanceResource) {
        if (groupVolumeSetsWithoutFQDN.isEmpty()) {
            LOGGER.info("There is no volume without fqdn to attach so we will not attach anything for this instance: {}", instanceResource);
            return List.of(instanceResource);
        } else {
            CloudResource groupVolumeSet = groupVolumeSetsWithoutFQDN.remove(0);
            LOGGER.info("There is a volume without fqdn so we will attach {} to this instance: {}", groupVolumeSet, instanceResource);
            return List.of(instanceResource, groupVolumeSet);
        }
    }

}
