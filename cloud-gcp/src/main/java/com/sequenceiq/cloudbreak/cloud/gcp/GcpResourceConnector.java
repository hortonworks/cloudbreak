package com.sequenceiq.cloudbreak.cloud.gcp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceNameService;
import com.sequenceiq.cloudbreak.cloud.template.AbstractResourceConnector;
import com.sequenceiq.cloudbreak.cloud.template.compute.ComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.group.GroupResourceService;
import com.sequenceiq.cloudbreak.cloud.template.init.ContextBuilders;
import com.sequenceiq.cloudbreak.cloud.template.network.NetworkResourceService;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GcpResourceConnector extends AbstractResourceConnector {

    @Inject
    private NetworkResourceService networkResourceService;

    @Inject
    private GroupResourceService groupResourceService;

    @Inject
    private ComputeResourceService computeResourceService;

    @Inject
    private ContextBuilders contextBuilders;

    @Override
    public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        return new TlsInfo(false);
    }

    @Override
    public String getStackTemplate() throws TemplatingNotSupportedException {
        throw new TemplatingNotSupportedException();
    }

    @Override
    public String getDBStackTemplate() throws TemplatingNotSupportedException {
        return "";
    }

    @Override
    public List<CloudResourceStatus> launchLoadBalancers(AuthenticatedContext auth, CloudStack stack, PersistenceNotifier persistenceNotifier)
            throws Exception {
        throw new UnsupportedOperationException("Load balancers are not supported for GCP.");
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources) {
        CloudContext cloudContext = auth.getCloudContext();
        Platform platform = cloudContext.getPlatform();
        Variant variant = cloudContext.getVariant();

        //context
        ResourceBuilderContext context = contextBuilders.get(platform).contextInit(cloudContext, auth, stack.getNetwork(), resources, true);

        //network
        context.addNetworkResources(networkResourceService.getNetworkResources(variant, resources));

        Group scalingGroup = getScalingGroup(getGroup(stack.getGroups(), getGroupName(stack)));

        //group
        context.addGroupResources(scalingGroup.getName(), groupResourceService.getGroupResources(variant, resources));

        //compute
        List<CloudResource> diskSets = resources.stream()
                .filter(cloudResource -> scalingGroup.getName().equalsIgnoreCase(cloudResource.getGroup()))
                .filter(cloudResource -> ResourceType.GCP_ATTACHED_DISKSET.equals(cloudResource.getType()))
                .filter(cloudResource -> StringUtils.isEmpty(cloudResource.getInstanceId()) || CommonStatus.DETACHED.equals(cloudResource.getStatus()))
                .collect(Collectors.toList());
        for (int i = 0; i < diskSets.size(); i++) {
            CloudResource cloudResource = diskSets.get(i);
            Optional.ofNullable(scalingGroup.getInstances().get(i))
                    .map(CloudInstance::getTemplate)
                    .map(InstanceTemplate::getPrivateId)
                    .ifPresent(privateId -> context.addComputeResources(privateId, List.of(cloudResource)));
        }
        return computeResourceService.buildResourcesForUpscale(context, auth, stack, Collections.singletonList(scalingGroup));
    }

    @Override
    public List<CloudResource> collectResourcesToRemove(AuthenticatedContext authenticatedContext, CloudStack stack,
            List<CloudResource> resources, List<CloudInstance> vms) {
        List<CloudResource> result = Lists.newArrayList();
        result.addAll(getDeletableResources(resources, vms));
        result.addAll(collectProviderSpecificResources(resources, vms));
        return result;
    }

    @Override
    protected Collection<CloudResource> getDeletableResources(Iterable<CloudResource> resources, Iterable<CloudInstance> instances) {
        Collection<CloudResource> result = new ArrayList<>();
        for (CloudInstance instance : instances) {
            String instanceId = instance.getInstanceId();
            for (CloudResource resource : resources) {
                if (instanceId.equalsIgnoreCase(resource.getName()) || instanceId.equalsIgnoreCase(resource.getInstanceId())) {
                    result.add(resource);
                }
            }
        }
        return result;
    }

    @Override
    protected List<CloudResource> collectProviderSpecificResources(List<CloudResource> resources, List<CloudInstance> vms) {
        List<CloudResource> result = new ArrayList<>();
        for (CloudInstance instance : vms) {
            String instanceId = instance.getInstanceId();
            String baseName = instanceId.substring(0, instanceId.lastIndexOf(CloudbreakResourceNameService.DELIMITER));
            for (CloudResource resource : resources) {
                if (resource.getType() == ResourceType.GCP_RESERVED_IP && resource.getName().startsWith(baseName)) {
                    result.add(resource);
                }
            }
        }
        return result;
    }

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        return List.of();
    }
}
