package com.sequenceiq.cloudbreak.structuredevent.converter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
import com.sequenceiq.cloudbreak.service.customconfigs.CustomConfigurationsService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbServerConfigurer;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.InstanceGroupType;

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
    private CustomConfigurationsToCustomConfigurationsDetailsConverter customConfigurationsToCustomConfigurationsDetailsConverter;

    @Inject
    private RedbeamsDbServerConfigurer dbServerConfigurer;

    @Inject
    private EmbeddedDatabaseService embeddedDatabaseService;

    @Inject
    private CustomConfigurationsService customConfigurationsService;

    public StackDetails convert(StackView source, ClusterView cluster, List<InstanceGroupDto> instanceGroupDtos) {
        StackDetails stackDetails = new StackDetails();
        stackDetails.setId(source.getId());
        stackDetails.setName(source.getName());
        stackDetails.setTunnel(source.getTunnel().name());
        stackDetails.setType(source.getType().name());
        stackDetails.setRegion(source.getRegion());
        stackDetails.setAvailabilityZone(source.getAvailabilityZone());
        stackDetails.setPlatformVariant(source.getPlatformVariant());
        stackDetails.setMultiAz(getMultiAz(instanceGroupDtos));
        stackDetails.setDescription(source.getDescription());
        stackDetails.setCloudPlatform(source.getCloudPlatform());
        stackDetails.setStatus(source.getStatus().name());
        stackDetails.setJavaVersion(source.getJavaVersion());
        stackDetails.setMultiAz(source.isMultiAz());
        if (source.getDetailedStatus() != null) {
            stackDetails.setDetailedStatus(source.getDetailedStatus().name());
        }
        if (cluster != null && cluster.getCustomConfigurations() != null) {
            CustomConfigurations customConfigurationsWithConfigurations = customConfigurationsService.getByCrn(cluster.getCustomConfigurations().getCrn());
            stackDetails.setCustomConfigurations(customConfigurationsToCustomConfigurationsDetailsConverter.convert(customConfigurationsWithConfigurations));
        }
        stackDetails.setStatusReason(source.getStatusReason());
        stackDetails.setInstanceGroups(
                instanceGroupDtos.stream()
                        .map(ig -> instanceGroupToInstanceGroupDetailsConverter.convert(ig.getInstanceGroup(), ig.getInstanceMetadataViews().size()))
                        .collect(Collectors.toList()));
        stackDetails.setTags(source.getTags());
        convertComponents(stackDetails, source);
        convertDatabaseType(stackDetails, cluster, getGatewayGroup(instanceGroupDtos));
        return stackDetails;
    }

    private boolean getMultiAz(List<InstanceGroupDto> instanceGroupDtos) {
        return instanceGroupDtos
                .stream()
                .flatMap(e -> getSubnetIds(e.getInstanceGroup()).stream())
                .distinct()
                .count() > 1;
    }

    private List<String> getSubnetIds(InstanceGroupView instanceGroupView) {
        if (instanceGroupView.getInstanceGroupNetwork() != null) {
            Json attributes = instanceGroupView.getInstanceGroupNetwork().getAttributes();
            if (attributes != null && attributes.getMap() != null) {
                return (List<String>) attributes.getMap().getOrDefault(NetworkConstants.SUBNET_IDS, List.of());
            }
        }
        return List.of();
    }

    private void convertComponents(StackDetails stackDetails, StackView stack) {
        Long stackId = stack.getId();
        try {
            Image image = componentConfigProviderService.getImage(stackId);
            stackDetails.setImage(imageToImageDetailsConverter.convert(image));
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.warn("Image not found! {}", e.getMessage());
        }
    }

    private void convertDatabaseType(StackDetails stackDetails, ClusterView cluster, Optional<InstanceGroupView> gatewayGroup) {
        try {
            if (dbServerConfigurer.isRemoteDatabaseRequested(cluster.getDatabaseServerCrn())) {
                stackDetails.setDatabaseType(EXTERNAL_DB);
            } else {
                if (embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(cluster, gatewayGroup)) {
                    stackDetails.setDatabaseType(ON_ATTACHED_VOLUME);
                } else {
                    stackDetails.setDatabaseType(ON_ROOT_VOLUME);
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Database type cannot be found: {}", ex.getMessage());
            stackDetails.setDatabaseType(UNKNONW);
        }
    }

    public Optional<InstanceGroupView> getGatewayGroup(Collection<InstanceGroupDto> instanceGroups) {
        return instanceGroups.stream().filter(ig -> ig.getInstanceGroup().getInstanceGroupType() == InstanceGroupType.GATEWAY)
                .findFirst().map(ig -> ig.getInstanceGroup());
    }
}
