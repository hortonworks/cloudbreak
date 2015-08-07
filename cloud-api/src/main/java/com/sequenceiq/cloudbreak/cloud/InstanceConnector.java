package com.sequenceiq.cloudbreak.cloud;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

public interface InstanceConnector {

    // VM

    String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm);

    List<CloudVmInstanceStatus> collectMetadata(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<InstanceTemplate> vms);

    List<CloudVmInstanceStatus> start(AuthenticatedContext ac, List<CloudInstance> vms);

    List<CloudVmInstanceStatus> stop(AuthenticatedContext ac, List<CloudInstance> vms);

    List<CloudVmInstanceStatus> check(AuthenticatedContext ac, List<CloudInstance> vms);


}
