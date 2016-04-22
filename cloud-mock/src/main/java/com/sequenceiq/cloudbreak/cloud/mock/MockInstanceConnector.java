package com.sequenceiq.cloudbreak.cloud.mock;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@Service
public class MockInstanceConnector implements InstanceConnector {

    private static final String CB_FINGERPRINT = "ce:50:66:23:96:08:04:ea:01:62:9b:18:f9:ee:ac:aa (RSA)";

    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) throws Exception {
        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = new ArrayList<>();
        for (CloudInstance instance : vms) {
            CloudVmInstanceStatus instanceStatus = new CloudVmInstanceStatus(instance, InstanceStatus.CREATED);
            cloudVmInstanceStatuses.add(instanceStatus);
        }
        return cloudVmInstanceStatuses;
    }

    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) throws Exception {
        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = new ArrayList<>();
        for (CloudInstance instance : vms) {
            CloudVmInstanceStatus instanceStatus = new CloudVmInstanceStatus(instance, InstanceStatus.STOPPED);
            cloudVmInstanceStatuses.add(instanceStatus);
        }
        return cloudVmInstanceStatuses;
    }

    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext authenticatedContext, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = new ArrayList<>();
        for (CloudInstance instance : vms) {
            CloudVmInstanceStatus instanceStatus = new CloudVmInstanceStatus(instance, InstanceStatus.STARTED);
            cloudVmInstanceStatuses.add(instanceStatus);
        }
        return cloudVmInstanceStatuses;
    }

    @Override
    public String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm) {
        return CB_FINGERPRINT;
    }
}
