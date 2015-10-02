package com.sequenceiq.cloudbreak.cloud;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

public interface MetadataCollector {

    List<CloudVmInstanceStatus> collect(AuthenticatedContext authContext, CloudStack cloudStack, List<CloudResource> resources, List<InstanceTemplate> vms);
}
