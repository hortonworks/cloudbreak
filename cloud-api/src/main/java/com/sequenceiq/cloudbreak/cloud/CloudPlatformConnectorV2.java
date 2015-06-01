package com.sequenceiq.cloudbreak.cloud;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.StackContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Instance;


public interface CloudPlatformConnectorV2 {

    String getCloudPlatform();

    AuthenticatedContext authenticate(StackContext stackContext, CloudCredential cloudCredential);

        List<CloudResourceStatus> launchStack(AuthenticatedContext authenticatedContext, CloudStack stack);

    List<CloudResourceStatus> checkResourcesState(AuthenticatedContext authenticatedContext, List<CloudResource> resources);

    List<CloudResourceStatus> terminateStack(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources);

    List<CloudVmInstanceStatus> collectVmMetadata(AuthenticatedContext authenticatedContext, List<CloudResource> resources);

    List<CloudVmInstanceStatus> start(AuthenticatedContext ac, List<CloudResource> resources, List<Instance> vms);

    List<CloudVmInstanceStatus> stop(AuthenticatedContext ac, List<CloudResource> resources, List<Instance> vms);

    List<CloudResourceStatus> upscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources, int adjustment);

    List<CloudResourceStatus> downscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources, List<Instance> vms);

    //e.g. security rules
    List<CloudResourceStatus> updateResources(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources);

}
