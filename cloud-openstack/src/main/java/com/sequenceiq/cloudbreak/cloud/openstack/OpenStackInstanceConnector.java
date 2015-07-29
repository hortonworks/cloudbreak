package com.sequenceiq.cloudbreak.cloud.openstack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Action;
import org.openstack4j.model.compute.ActionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@Service
public class OpenStackInstanceConnector implements InstanceConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackInstanceConnector.class);

    @Inject
    private OpenStackClient openStackClient;

    @Inject
    private OpenStackMetadataCollector metadataCollector;

    @Override
    public Set<String> getSSHFingerprints(AuthenticatedContext authenticatedContext, CloudInstance vm) {
        return null;
    }

    @Override
    public List<CloudVmInstanceStatus> collectMetadata(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<InstanceTemplate> vms) {
        return metadataCollector.collectVmMetadata(authenticatedContext, resources, vms);
    }

    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext ac, List<CloudInstance> vms) {
        return executeAction(ac, vms, Action.START);
    }

    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext ac, List<CloudInstance> vms) {
        return executeAction(ac, vms, Action.STOP);
    }

    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        return null;
    }

    private List<CloudVmInstanceStatus> executeAction(AuthenticatedContext ac, List<CloudInstance> cloudInstances, Action action) {
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();
        OSClient osClient = openStackClient.createOSClient(ac);
        for (CloudInstance cloudInstance : cloudInstances) {
            //TODO use real instance id
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
