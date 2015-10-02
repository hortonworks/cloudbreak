package com.sequenceiq.cloudbreak.cloud;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

public interface InstanceConnector {

    List<CloudVmInstanceStatus> start(AuthenticatedContext ac, CloudStack cloudStack, List<CloudResource> resources, List<CloudInstance> vms) throws Exception;

    List<CloudVmInstanceStatus> stop(AuthenticatedContext ac, CloudStack cloudStack, List<CloudResource> resources, List<CloudInstance> vms) throws Exception;

    MetadataCollector metadata();

    List<CloudVmInstanceStatus> check(AuthenticatedContext ac, CloudStack cloudStack, List<CloudInstance> vms);

    String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudStack cloudStack, CloudInstance vm);

}
