package com.sequenceiq.cloudbreak.structuredevent.converter;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbServerConfigurer;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;

@Component
public class StackToStackDetailsConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackToStackDetailsConverter.class);

    private static final String UNKNONW = "UNKNOWN";

    private static final String ON_ROOT_VOLUME = "ON_ROOT_VOLUME";

    private static final String ON_ATTACHED_VOLUME = "ON_ATTACHED_VOLUME";

    private static final String EXTERNAL_DB = "EXTERNAL_DB";

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private InstanceGroupToInstanceGroupDetailsConverter instanceGroupToInstanceGroupDetailsConverter;

    @Inject
    private ImageToImageDetailsConverter imageToImageDetailsConverter;

    @Inject
    private RedbeamsDbServerConfigurer dbServerConfigurer;

    @Inject
    private EmbeddedDatabaseService embeddedDatabaseService;

    public StackDetails convert(Stack source) {
        StackDetails stackDetails = new StackDetails();
        stackDetails.setId(source.getId());
        stackDetails.setName(source.getName());
        stackDetails.setTunnel(source.getTunnel().name());
        stackDetails.setType(source.getType().name());
        stackDetails.setRegion(source.getRegion());
        stackDetails.setAvailabilityZone(source.getAvailabilityZone());
        stackDetails.setPlatformVariant(source.getPlatformVariant());
        stackDetails.setMultiAz(getMultiAz(source));
        stackDetails.setDescription(source.getDescription());
        stackDetails.setCloudPlatform(source.cloudPlatform());
        stackDetails.setStatus(source.getStatus().name());
        if (source.getStackStatus() != null && source.getStackStatus().getDetailedStackStatus() != null) {
            stackDetails.setDetailedStatus(source.getStackStatus().getDetailedStackStatus().name());
        }
        stackDetails.setStatusReason(source.getStatusReason());
        stackDetails.setInstanceGroups(
                source.getInstanceGroups().stream()
                        .map(e -> instanceGroupToInstanceGroupDetailsConverter.convert(e))
                        .collect(Collectors.toList()));
        stackDetails.setTags(source.getTags());
        convertComponents(stackDetails, source);
        convertDatabaseType(stackDetails, source);
        return stackDetails;
    }

    private boolean getMultiAz(Stack stack) {
        return stack.getInstanceGroups()
                .stream()
                .flatMap(e -> getSubnetIds(e).stream())
                .distinct()
                .count() > 1;
    }

    private List<String> getSubnetIds(com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup e) {
        if (e.getInstanceGroupNetwork() != null) {
            Json attributes = e.getInstanceGroupNetwork().getAttributes();
            if (attributes != null && attributes.getMap() != null) {
                return (List<String>) attributes.getMap().getOrDefault(NetworkConstants.SUBNET_IDS, List.of());
            }
        }
        return List.of();
    }

    private void convertComponents(StackDetails stackDetails, Stack stack) {
        Long stackId = stack.getId();
        try {
            Image image = componentConfigProviderService.getImage(stackId);
            stackDetails.setImage(imageToImageDetailsConverter.convert(image));
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.warn("Image not found! {}", e.getMessage());
        }
    }

    private void convertDatabaseType(StackDetails stackDetails, Stack stack) {
        try {
            if (dbServerConfigurer.isRemoteDatabaseNeeded(stack.getCluster())) {
                stackDetails.setDatabaseType(EXTERNAL_DB);
            } else {
                if (embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(stack)) {
                    stackDetails.setDatabaseType(ON_ATTACHED_VOLUME);
                } else {
                    stackDetails.setDatabaseType(ON_ROOT_VOLUME);
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Database type cannot be found", ex.getMessage());
            stackDetails.setDatabaseType(UNKNONW);
        }
    }
}
