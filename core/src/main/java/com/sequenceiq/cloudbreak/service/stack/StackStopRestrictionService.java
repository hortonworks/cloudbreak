package com.sequenceiq.cloudbreak.service.stack;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.stack.StackStopRestrictionConfiguration.ServiceRoleGroup;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;

@Service
public class StackStopRestrictionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStopRestrictionService.class);

    @Inject
    private StackStopRestrictionConfiguration config;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private ClusterComponentConfigProvider clusterComponentProvider;

    @Inject
    private InstanceGroupEphemeralVolumeChecker ephemeralVolumeChecker;

    public StopRestrictionReason isInfrastructureStoppable(Stack stack) {
        if (!config.getRestrictedCloudPlatform().equals(stack.getCloudPlatform())) {
            return StopRestrictionReason.NONE;
        }

        String cbVersion = componentConfigProviderService.getCloudbreakDetails(stack.getId()).getVersion();
        String saltCbVersion = clusterComponentProvider.getSaltStateComponentCbVersion(stack.getCluster().getId());

        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            if (ephemeralVolumeChecker.instanceGroupContainsOnlyDatabaseAndEphemeralVolumes(instanceGroup)) {
                StopRestrictionReason ephemeralResult = checkEphemeralOnlyInstanceGroupStoppable(instanceGroup, cbVersion, saltCbVersion);
                if (ephemeralResult != StopRestrictionReason.NONE) {
                    return ephemeralResult;
                }
            } else {
                StopRestrictionReason nonEphemeralResult = checkNonEphemeralOnlyInstanceGroupStoppable(instanceGroup, cbVersion);
                if (nonEphemeralResult != StopRestrictionReason.NONE) {
                    return nonEphemeralResult;
                }
            }
        }
        LOGGER.debug("Stack [{}] is stoppable.", stack.getName());
        return StopRestrictionReason.NONE;
    }

    private StopRestrictionReason checkEphemeralOnlyInstanceGroupStoppable(InstanceGroup instanceGroup, String cbVersion, String saltCbVersion) {
        if (!isCbVersionBeforeMinVersion(cbVersion, config.getEphemeralOnlyMinVersion()) || !isSaltComponentCbVersionBeforeStopSupport(saltCbVersion)) {
            Set<ServiceComponent> serviceComponents = cmTemplateProcessorFactory.get(instanceGroup.getStack().getCluster().getBlueprint().getBlueprintText())
                    .getServiceComponentsByHostGroup().get(instanceGroup.getGroupName());
            if (!isEphemeralInstanceGroupStoppable(serviceComponents)) {
                LOGGER.info("Infrastructure cannot be stopped. Instances in group [{}] have ephemeral storage only " +
                        "and one or more services on the host group do not support stopping on ephemeral volumes.", instanceGroup.getGroupName());
                return StopRestrictionReason.EPHEMERAL_VOLUMES;
            } else {
                LOGGER.debug("Salt script version of ephemeral instance groups [{}] is above the required version and the service " +
                        "components running on the instance group are enabled to stop. The instance group is stoppable.", instanceGroup.getGroupName());
                return StopRestrictionReason.NONE;
            }
        } else {
            LOGGER.info("Infrastructure cannot be stopped. Instances in group [{}] have ephemeral storage only. " +
                            "Stopping clusters with ephemeral volume based host groups are only available in clusters " +
                            "created at least with Cloudbreak version [{}] or upgraded.", instanceGroup.getGroupName(), config.getEphemeralOnlyMinVersion());
            return StopRestrictionReason.EPHEMERAL_VOLUMES;
        }
    }

    private StopRestrictionReason checkNonEphemeralOnlyInstanceGroupStoppable(InstanceGroup instanceGroup, String cbVersion) {
        if (!isCbVersionBeforeMinVersion(cbVersion, config.getEphemeralCachingMinVersion())) {
            LOGGER.debug("Non ephemeral instance group [{}] was created with Cloudbbreak version above the required version. " +
                    "The instance group is stoppable.", instanceGroup.getGroupName());
            return StopRestrictionReason.NONE;
        } else if (TemporaryStorage.EPHEMERAL_VOLUMES.equals(instanceGroup.getTemplate().getTemporaryStorage())) {
            LOGGER.info("Infrastructure cannot be stopped. Group [{}] has ephemeral volume caching enabled. " +
                    "Stopping clusters with ephemeral volume caching enabled are only available in clusters " +
                    "created at least with Cloudbreak version [{}].", instanceGroup.getGroupName(), config.getEphemeralCachingMinVersion());
            return StopRestrictionReason.EPHEMERAL_VOLUME_CACHING;
        } else if (ephemeralVolumeChecker.instanceGroupContainsEphemeralVolumes(instanceGroup)) {
            LOGGER.info("Infrastructure cannot be stopped. Instances in group [{}] have ephemeral storage." +
                    "Stopping clusters with ephemeral storage instances are only available in clusters " +
                    "created at least with Cloudbreak version [{}].", instanceGroup.getGroupName(), config.getEphemeralCachingMinVersion());
            return StopRestrictionReason.EPHEMERAL_VOLUMES;
        } else {
            LOGGER.debug("Non ephemeral instance group [{}] is stoppable.", instanceGroup.getGroupName());
            return StopRestrictionReason.NONE;
        }
    }

    private boolean isEphemeralInstanceGroupStoppable(Set<ServiceComponent> serviceComponents) {
        return config.getPermittedServiceRoleGroups().stream()
                .anyMatch(serviceRoleGroup -> areServiceComponentsPermitted(serviceRoleGroup, serviceComponents)
                        && areRequiredServiceRolesPresent(serviceRoleGroup, serviceComponents));
    }

    private boolean areServiceComponentsPermitted(ServiceRoleGroup serviceRoleGroup, Set<ServiceComponent> presentServiceComponents) {
        Set<ServiceComponent> permittedServiceComponents = serviceRolesToServiceComponents(serviceRoleGroup.getServiceRoles());
        Set<String> permittedComponents = serviceRoleGroup.getRoles().stream()
                .map(ServiceRoleGroup.ServiceRole::getRole)
                .collect(Collectors.toSet());

        return presentServiceComponents.stream()
                .allMatch(serviceComponent -> permittedServiceComponents.contains(serviceComponent)
                        || permittedComponents.contains(serviceComponent.getComponent()));
    }

    private boolean areRequiredServiceRolesPresent(ServiceRoleGroup serviceRoleGroup, Set<ServiceComponent> presentServiceComponents) {
        Set<ServiceComponent> requiredServiceComponents = serviceRolesToServiceComponents(serviceRoleGroup.getRequiredServiceRoles());

        Set<String> requiredComponents = serviceRoleGroup.getRequiredRoles().stream()
                .map(ServiceRoleGroup.ServiceRole::getRole)
                .collect(Collectors.toSet());
        Set<String> presentComponents = presentServiceComponents.stream()
                .map(ServiceComponent::getComponent)
                .collect(Collectors.toSet());

        return presentServiceComponents.containsAll(requiredServiceComponents) && presentComponents.containsAll(requiredComponents);
    }

    private Set<ServiceComponent> serviceRolesToServiceComponents(Set<ServiceRoleGroup.ServiceRole> serviceRoles) {
        return serviceRoles.stream()
                .map(serviceRole -> ServiceComponent.of(serviceRole.getService(), serviceRole.getRole()))
                .collect(Collectors.toSet());
    }

    private boolean isSaltComponentCbVersionBeforeStopSupport(String saltCbVersion) {
        return saltCbVersion == null || isCbVersionBeforeMinVersion(saltCbVersion, config.getEphemeralOnlyMinVersion());
    }

    private boolean isCbVersionBeforeMinVersion(String cbVersion, String minVersion) {
        VersionComparator versionComparator = new VersionComparator();
        String version = StringUtils.substringBefore(cbVersion, "-");
        int compare = versionComparator.compare(() -> version, () -> minVersion);
        return compare < 0;
    }
}
