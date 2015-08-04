package com.sequenceiq.cloudbreak.cloud;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Instance;

public interface InstanceConnector {

    // VM

    Set<String> getSSHFingerprints(AuthenticatedContext authenticatedContext, Instance vm);

    List<CloudVmInstanceStatus> collectMetadata(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<Instance> vms);

    List<CloudVmInstanceStatus> start(AuthenticatedContext ac, List<Instance> vms);

    List<CloudVmInstanceStatus> stop(AuthenticatedContext ac, List<Instance> vms);

    List<CloudVmInstanceStatus> check(AuthenticatedContext ac, List<CloudResource> resources, List<Instance> vms);


}
