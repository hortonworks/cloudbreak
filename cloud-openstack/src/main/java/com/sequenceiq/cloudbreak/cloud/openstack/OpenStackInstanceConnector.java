package com.sequenceiq.cloudbreak.cloud.openstack;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Action;
import org.openstack4j.model.compute.ActionResponse;
import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.openstack.status.NovaInstanceStatus;

@Service
public class OpenStackInstanceConnector implements InstanceConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackInstanceConnector.class);
    private static final int CONSOLE_OUTPUT_LINES = Integer.MAX_VALUE;

    @Inject
    private OpenStackClient openStackClient;
    @Inject
    private OpenStackMetadataCollector metadataCollector;

    @Override
    public String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudStack cloudStack, CloudInstance vm) {
        OSClient osClient = openStackClient.createOSClient(authenticatedContext);
        return osClient.compute().servers().getConsoleOutput(vm.getInstanceId(), CONSOLE_OUTPUT_LINES);
    }

    @Override
    public MetadataCollector metadata() {
        return metadataCollector;
    }

    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext ac, CloudStack cloudStack, List<CloudResource> resources, List<CloudInstance> vms) {
        return executeAction(ac, vms, Action.START);
    }

    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext ac, CloudStack cloudStack, List<CloudResource> resources, List<CloudInstance> vms) {
        return executeAction(ac, vms, Action.STOP);
    }

    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext ac, CloudStack cloudStack, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();
        OSClient osClient = openStackClient.createOSClient(ac);
        for (CloudInstance vm : vms) {
            Server server = osClient.compute().servers().get(vm.getInstanceId());
            if (server == null) {
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.TERMINATED));
            } else {
                statuses.add(new CloudVmInstanceStatus(vm, NovaInstanceStatus.get(server)));
            }
        }
        return statuses;
    }

    private List<CloudVmInstanceStatus> executeAction(AuthenticatedContext ac, List<CloudInstance> cloudInstances, Action action) {
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();
        OSClient osClient = openStackClient.createOSClient(ac);
        for (CloudInstance cloudInstance : cloudInstances) {
            ActionResponse actionResponse = osClient.compute().servers().action(cloudInstance.getInstanceId(), action);
            if (actionResponse.isSuccess()) {
                statuses.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.IN_PROGRESS));
            } else {
                statuses.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.FAILED, actionResponse.getFault()));
            }
        }
        return statuses;
    }
}
