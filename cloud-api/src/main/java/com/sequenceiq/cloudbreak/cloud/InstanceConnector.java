package com.sequenceiq.cloudbreak.cloud;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Instance;

public interface InstanceConnector {
    // VM

    List<CloudVmInstanceStatus> metaData(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<Instance> vms);

    List<CloudVmInstanceStatus> start(AuthenticatedContext ac, List<CloudResource> resources, List<Instance> vms);

    List<CloudVmInstanceStatus> stop(AuthenticatedContext ac, List<CloudResource> resources, List<Instance> vms);

    List<CloudVmInstanceStatus> check(AuthenticatedContext ac, List<CloudResource> resources, List<Instance> vms);


}
