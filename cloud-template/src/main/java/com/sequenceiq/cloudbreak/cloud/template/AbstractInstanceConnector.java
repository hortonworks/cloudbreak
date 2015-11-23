package com.sequenceiq.cloudbreak.cloud.template;

import java.util.List;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.template.compute.ComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ContextBuilders;

/**
 * Abstract base implementation of {@link InstanceConnector} for cloud provider which do not have template based deployments. It provides
 * the functionality to call the resource builders in order to stop and start the resources. <b>Only compute</b> resources can be stopped/started.
 */
public abstract class AbstractInstanceConnector implements InstanceConnector {

    @Inject
    private ComputeResourceService computeResourceService;
    @Inject
    private ContextBuilders contextBuilders;

    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) throws Exception {
        CloudContext cloudContext = ac.getCloudContext();
        Platform platform = cloudContext.getPlatform();

        //context
        ResourceBuilderContext context = contextBuilders.get(platform).contextInit(cloudContext, ac, resources, false);

        //compute
        return computeResourceService.stopInstances(context, ac, resources, vms);
    }

    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) throws Exception {
        CloudContext cloudContext = ac.getCloudContext();
        Platform platform = cloudContext.getPlatform();

        //context
        ResourceBuilderContext context = contextBuilders.get(platform).contextInit(cloudContext, ac, resources, true);

        //compute
        return computeResourceService.startInstances(context, ac, resources, vms);
    }

}
