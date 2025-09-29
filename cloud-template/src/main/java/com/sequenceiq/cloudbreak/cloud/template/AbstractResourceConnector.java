package com.sequenceiq.cloudbreak.cloud.template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.StringUtils;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.model.database.ExternalDatabaseParameters;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.template.authentication.AuthenticationResourceService;
import com.sequenceiq.cloudbreak.cloud.template.compute.ComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.template.compute.DatabaseServerCheckerService;
import com.sequenceiq.cloudbreak.cloud.template.compute.DatabaseServerLaunchService;
import com.sequenceiq.cloudbreak.cloud.template.compute.DatabaseServerStartService;
import com.sequenceiq.cloudbreak.cloud.template.compute.DatabaseServerStopService;
import com.sequenceiq.cloudbreak.cloud.template.compute.DatabaseServerTerminateService;
import com.sequenceiq.cloudbreak.cloud.template.compute.DatabaseServerUpgradeService;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.group.GroupResourceService;
import com.sequenceiq.cloudbreak.cloud.template.init.ContextBuilders;
import com.sequenceiq.cloudbreak.cloud.template.loadbalancer.LoadBalancerResourceService;
import com.sequenceiq.cloudbreak.cloud.template.network.NetworkResourceService;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Abstract base implementation of {@link ResourceConnector} for cloud provider which do not have template based deployments. It provides the
 * functionality to call the resource builders in order starting from the {@link NetworkResourceBuilder} and continuing with the
 * {@link ComputeResourceBuilder}. Before calling any resource builder it constructs a generic {@link ResourceBuilderContext}. This context object
 * will be extended with the created resources as the builder finish creating them. The resources are grouped by private id.
 * <br>
 * Compute resource can be rolled back based on the different failure policies configured. Network resource failure immediately results in a failing deployment.
 */
public abstract class AbstractResourceConnector implements ResourceConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractResourceConnector.class);

    @Inject
    private NetworkResourceService networkResourceService;

    @Inject
    private AuthenticationResourceService authenticationResourceService;

    @Inject
    private GroupResourceService groupResourceService;

    @Inject
    private ComputeResourceService computeResourceService;

    @Inject
    private LoadBalancerResourceService loadBalancerResourceService;

    @Inject
    private DatabaseServerTerminateService databaseServerTerminateService;

    @Inject
    private DatabaseServerLaunchService databaseServerLaunchService;

    @Inject
    private DatabaseServerUpgradeService databaseServerUpgradeService;

    @Inject
    private DatabaseServerCheckerService databaseServerCheckerService;

    @Inject
    private DatabaseServerStopService databaseServerStopService;

    @Inject
    private DatabaseServerStartService databaseServerStartService;

    @Inject
    private ContextBuilders contextBuilders;

    @Inject
    private CloudResourceHelper cloudResourceHelper;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext auth, CloudStack stack, PersistenceNotifier notifier,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) throws Exception {
        LOGGER.info("Launch stack ({}) with adjustment type and threshold: {}", auth.getCloudContext().getName(), adjustmentTypeWithThreshold);
        CloudContext cloudContext = auth.getCloudContext();
        Platform platform = cloudContext.getPlatform();

        //context
        ResourceBuilderContext context = contextBuilders.get(platform).contextInit(cloudContext, auth, stack.getNetwork(), true);

        //authentication
        List<CloudResourceStatus> authenticationStatuses = authenticationResourceService.buildResources(context, auth, stack);
        context.addAuthenticationResources(getCloudResources(authenticationStatuses));

        //network
        List<CloudResourceStatus> cloudResourceStatuses = networkResourceService.buildResources(context, auth, stack.getNetwork(), stack.getCloudSecurity());
        context.addNetworkResources(getCloudResources(cloudResourceStatuses));

        //group
        List<CloudResourceStatus> groupStatuses = groupResourceService.buildResources(context, auth, stack.getGroups(), stack.getNetwork(),
                stack.getCloudSecurity());
        cloudResourceStatuses.addAll(groupStatuses);

        //compute
        List<CloudResourceStatus> computeStatuses = computeResourceService.buildResourcesForLaunch(context, auth, stack, adjustmentTypeWithThreshold);
        cloudResourceStatuses.addAll(computeStatuses);

        return cloudResourceStatuses;
    }

    @Override
    public List<CloudResourceStatus> launchDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack,
            PersistenceNotifier persistenceNotifier) throws Exception {
        List<CloudResource> cloudResources = databaseServerLaunchService.launch(authenticatedContext, stack, persistenceNotifier);
        return cloudResources.stream()
                .map(e -> new CloudResourceStatus(e, ResourceStatus.CREATED))
                .collect(Collectors.toList());
    }

    @Override
    public void validateUpgradeDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack, TargetMajorVersion targetMajorVersion) {
    }

    @Override
    public List<CloudResourceStatus> launchValidateUpgradeDatabaseServerResources(AuthenticatedContext authenticatedContext, DatabaseStack stack,
            TargetMajorVersion targetMajorVersion, DatabaseStack migratedDbStack, PersistenceNotifier persistenceNotifier) {
        return List.of();
    }

    @Override
    public void upgradeDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack originalStack, DatabaseStack stack,
            PersistenceNotifier persistenceNotifier, TargetMajorVersion targetMajorVersion, List<CloudResource> resources) throws Exception {
        databaseServerUpgradeService.upgrade(authenticatedContext, stack, persistenceNotifier, targetMajorVersion);
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext auth, CloudStack stack, List<CloudResource> cloudResources) throws Exception {
        CloudContext cloudContext = auth.getCloudContext();
        Platform platform = cloudContext.getPlatform();

        //context
        ResourceBuilderContext context = contextBuilders.get(platform).contextInit(cloudContext, auth, stack.getNetwork(), false);

        //loadBalancer
        List<CloudResourceStatus> cloudResourceStatuses = loadBalancerResourceService.deleteResources(context, auth, cloudResources, false);

        //compute
        List<CloudResourceStatus> computeStatuses = computeResourceService.deleteResources(context, auth, cloudResources, false, true);
        cloudResourceStatuses.addAll(computeStatuses);

        //group
        List<CloudResourceStatus> groupStatuses = groupResourceService.deleteResources(context, auth, cloudResources, stack.getNetwork(), false);
        cloudResourceStatuses.addAll(groupStatuses);

        //network
        List<CloudResourceStatus> networkStatuses = networkResourceService.deleteResources(context, auth, cloudResources, stack.getNetwork(), false);
        cloudResourceStatuses.addAll(networkStatuses);

        //authentication
        List<CloudResourceStatus> authenticationStatuses = authenticationResourceService.deleteResources(context, auth, cloudResources, stack, false);
        cloudResourceStatuses.addAll(authenticationStatuses);

        return cloudResourceStatuses;
    }

    @Override
    public List<CloudResourceStatus> terminateDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack,
            List<CloudResource> resources, PersistenceNotifier persistenceNotifier, boolean force) throws Exception {
        List<CloudResource> cloudResources = databaseServerTerminateService.terminate(authenticatedContext, stack, persistenceNotifier);
        return cloudResources.stream()
                .map(e -> new CloudResourceStatus(e, ResourceStatus.DELETED))
                .collect(Collectors.toList());
    }

    @Override
    public void startDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack) throws Exception {
        databaseServerStartService.start(authenticatedContext, stack);
    }

    @Override
    public void stopDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack) throws Exception {
        databaseServerStopService.stop(authenticatedContext, stack);
    }

    @Override
    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 5000))
    public ExternalDatabaseStatus getDatabaseServerStatus(AuthenticatedContext authenticatedContext, DatabaseStack stack) {
        return databaseServerCheckerService.check(authenticatedContext, stack);
    }

    @Override
    public ExternalDatabaseParameters getDatabaseServerParameters(AuthenticatedContext authenticatedContext, DatabaseStack stack) throws Exception {
        ExternalDatabaseStatus databaseStatus = databaseServerCheckerService.check(authenticatedContext, stack);
        return new ExternalDatabaseParameters(databaseStatus, null, null);
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) throws QuotaExceededException {
        LOGGER.info("Upscale stack ({}) with adjustment type and threshold: {}", auth.getCloudContext().getName(), adjustmentTypeWithThreshold);
        CloudContext cloudContext = auth.getCloudContext();
        Platform platform = cloudContext.getPlatform();
        Variant variant = cloudContext.getVariant();

        //context
        ResourceBuilderContext context = contextBuilders.get(platform).contextInit(cloudContext, auth, stack.getNetwork(), true);

        //authentication
        context.addAuthenticationResources(authenticationResourceService.getAuthenticationResources(variant, resources));

        //network
        context.addNetworkResources(networkResourceService.getNetworkResources(variant, resources));

        Set<Group> scalingGroups = getScalingGroups(getGroups(stack.getGroups(), getGroupNames(stack)));

        List<CloudResource> reattachableVolumeSets = new ArrayList<>();
        //group
        for (Group scalingGroup : scalingGroups) {
            context.addGroupResources(scalingGroup.getName(), groupResourceService.getGroupResources(variant, resources));
            reattachableVolumeSets = diskReattachment(resources, scalingGroup, context);
        }

        cloudResourceHelper.updateDeleteOnTerminationFlag(reattachableVolumeSets, false, cloudContext);
        //compute
        List<CloudResourceStatus> cloudResourceStatuses = computeResourceService.buildResourcesForUpscale(context, auth, stack,
                scalingGroups, adjustmentTypeWithThreshold);
        cloudResourceHelper.updateDeleteOnTerminationFlag(reattachableVolumeSets, true, cloudContext);
        return cloudResourceStatuses;
    }

    protected List<CloudResource> diskReattachment(List<CloudResource> resources, Group scalingGroup, ResourceBuilderContext context) {
        LOGGER.info("Disk reattachment with resources: {} for group: {}", resources, scalingGroup);
        List<CloudResource> diskSets = resources.stream()
                .filter(cloudResource -> scalingGroup.getName().equalsIgnoreCase(cloudResource.getGroup()))
                .filter(cloudResource -> getDiskResourceType().equals(cloudResource.getType()))
                .filter(cloudResource -> StringUtils.isEmpty(cloudResource.getInstanceId()) || CommonStatus.DETACHED.equals(cloudResource.getStatus()))
                .collect(Collectors.toList());
        for (CloudResource cloudResource : diskSets) {
            VolumeSetAttributes volumeSetAttributes = cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
            if (volumeSetAttributes != null) {
                if (volumeSetAttributes.getDiscoveryFQDN() != null) {
                    String discoveryFQDN = volumeSetAttributes.getDiscoveryFQDN();
                    scalingGroup.getInstances().stream()
                            .filter(cloudInstance -> discoveryFQDN.equals(cloudInstance.getStringParameter(CloudInstance.FQDN)))
                            .findFirst()
                            .map(CloudInstance::getTemplate)
                            .map(InstanceTemplate::getPrivateId)
                            .ifPresent(privateId -> context.addComputeResources(privateId, List.of(cloudResource)));
                } else {
                    LOGGER.error("There is no FQDN set for volume of the disk resource [name: {}] in the database. The disk cannot be reused for reattachment.",
                            cloudResource.getName());
                }
            }
        }
        return diskSets;
    }

    protected ResourceType getDiskResourceType() {
        throw new IllegalArgumentException("Please override the disk resource type or implement the provider specified disk reattachment");
    }

    @Override
    public List<CloudResource> collectResourcesToRemove(AuthenticatedContext authenticatedContext, CloudStack stack,
            List<CloudResource> resources, List<CloudInstance> vms) {
        List<CloudResource> result = Lists.newArrayList();
        result.addAll(getDeletableResources(resources, vms));
        result.addAll(collectProviderSpecificResources(resources, vms));
        return result;
    }

    protected abstract List<CloudResource> collectProviderSpecificResources(List<CloudResource> resources, List<CloudInstance> vms);

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms,
            List<CloudResource> resourcesToRemove) {
        LOGGER.info("Downscale stack with resources: {} vms: {}, resources to remove: {}", resources, vms, resourcesToRemove);
        CloudContext cloudContext = auth.getCloudContext();
        Platform platform = cloudContext.getPlatform();

        //context
        ResourceBuilderContext context = contextBuilders.get(platform).contextInit(cloudContext, auth, stack.getNetwork(), false);

        //compute
        return computeResourceService.deleteResources(context, auth, resourcesToRemove, true, true);
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources,
            UpdateType type, Optional<String> group) throws Exception {
        LOGGER.info("Update stack with resources: {}", resources);
        if (type.equals(UpdateType.VERTICAL_SCALE_WITHOUT_INSTANCES) || type.equals(UpdateType.PROVIDER_TEMPLATE_UPDATE)) {
            LOGGER.debug("Update type is {}, no further action required.", type);
            return new ArrayList<>();
        }
        CloudContext cloudContext = auth.getCloudContext();
        Platform platform = cloudContext.getPlatform();
        Variant variant = cloudContext.getVariant();

        //context
        ResourceBuilderContext context = contextBuilders.get(platform).contextInit(cloudContext, auth, stack.getNetwork(), true);

        //group
        List<CloudResource> groupResources = groupResourceService.getGroupResources(variant, resources);
        List<CloudResourceStatus> cloudResourceStatuses = groupResourceService.update(context, auth, stack.getNetwork(),
                stack.getCloudSecurity(), groupResources);

        //network
        List<CloudResource> networkResources = networkResourceService.getNetworkResources(variant, resources);
        List<CloudResourceStatus> networkStatuses = networkResourceService.update(context, auth, stack.getNetwork(),
                stack.getCloudSecurity(), networkResources);
        cloudResourceStatuses.addAll(networkStatuses);

        //authentication
        List<CloudResource> authenticationResources = authenticationResourceService.getAuthenticationResources(variant, resources);
        List<CloudResourceStatus> authenticationStatuses = authenticationResourceService.update(context, auth, stack, authenticationResources);
        cloudResourceStatuses.addAll(authenticationStatuses);

        //compute
        List<CloudResource> computeResources = computeResourceService.getComputeResources(variant, resources);
        List<CloudResourceStatus> computeStatuses = computeResourceService.update(context, auth, stack, computeResources, group, type);
        cloudResourceStatuses.addAll(computeStatuses);

        return cloudResourceStatuses;
    }

    @Override
    public void checkUpdate(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) throws Exception {
    }

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        throw new UnsupportedOperationException();
    }

    private Collection<CloudResource> getCloudResources(Iterable<CloudResourceStatus> resourceStatuses) {
        Collection<CloudResource> resources = new ArrayList<>();
        for (CloudResourceStatus status : resourceStatuses) {
            resources.add(status.getCloudResource());
        }
        return resources;
    }

    protected DatabaseServerCheckerService databaseServerCheckerService() {
        return databaseServerCheckerService;
    }

    protected Set<Group> getScalingGroups(Set<Group> scalingGroups) {
        Set<Group> groups = new HashSet<>();
        for (Group scalingGroup : scalingGroups) {
            List<CloudInstance> instances = new ArrayList<>(scalingGroup.getInstances());
            instances.removeIf(cloudInstance -> InstanceStatus.CREATE_REQUESTED != cloudInstance.getTemplate().getStatus());
            groups.add(Group.builder()
                    .withName(scalingGroup.getName())
                    .withType(scalingGroup.getType())
                    .withInstances(instances)
                    .withSecurity(scalingGroup.getSecurity())
                    .withInstanceAuthentication(scalingGroup.getInstanceAuthentication())
                    .withLoginUserName(scalingGroup.getInstanceAuthentication().getLoginUserName())
                    .withPublicKey(scalingGroup.getInstanceAuthentication().getPublicKey())
                    .withRootVolumeSize(scalingGroup.getRootVolumeSize())
                    .withIdentity(scalingGroup.getIdentity())
                    .withNetwork(scalingGroup.getNetwork())
                    .withTags(scalingGroup.getTags())
                    .withRootVolumeType(scalingGroup.getRootVolumeType())
                    .build());
        }
        return groups;
    }

    protected Set<Group> getGroups(Iterable<Group> groups, Set<String> groupNames) {
        Set<Group> resultGroup = new HashSet<>();
        for (Group group : groups) {
            if (groupNames.contains(group.getName())) {
                resultGroup.add(group);
            }
        }
        return resultGroup;
    }

    protected Set<String> getGroupNames(CloudStack stack) {
        Set<String> groupNames = new HashSet<>();
        for (Group group : stack.getGroups()) {
            for (CloudInstance instance : group.getInstances()) {
                InstanceTemplate instanceTemplate = instance.getTemplate();
                if (InstanceStatus.CREATE_REQUESTED == instanceTemplate.getStatus()) {
                    groupNames.add(instanceTemplate.getGroupName());
                }
            }
        }
        return groupNames;
    }

    protected Collection<CloudResource> getDeletableResources(Iterable<CloudResource> resources, Iterable<CloudInstance> instances) {
        Collection<CloudResource> result = new ArrayList<>();
        for (CloudInstance instance : instances) {
            for (CloudResource resource : resources) {
                if (isCloudResourceAndCloudInstanceEquals(instance, resource)) {
                    result.add(resource);
                }
            }
        }
        return result;
    }

    protected boolean isCloudResourceAndCloudInstanceEquals(CloudInstance instance, CloudResource resource) {
        return instance.getInstanceId().equalsIgnoreCase(resource.getName());
    }

    @Override
    public void cleanupValidateUpgradeDatabaseServerResources(AuthenticatedContext authenticatedContext, DatabaseStack stack, List<CloudResource> resources,
            PersistenceNotifier persistenceNotifier) throws Exception {

    }
}