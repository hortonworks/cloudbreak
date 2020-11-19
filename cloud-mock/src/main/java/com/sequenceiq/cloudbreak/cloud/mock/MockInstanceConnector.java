package com.sequenceiq.cloudbreak.cloud.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@Service
public class MockInstanceConnector implements InstanceConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockInstanceConnector.class);

    private static final String CB_FINGERPRINT = "ce:50:66:23:96:08:04:ea:01:62:9b:18:f9:ee:ac:aa (RSA)";

    @Inject
    private MockUrlFactory mockUrlFactory;

    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        LOGGER.info("start instances on mock spi");
        CloudVmInstanceStatus[] vmInstanceStatuses = mockUrlFactory.get(authenticatedContext, "/spi/start_instances")
                .post(Entity.entity(vms, MediaType.APPLICATION_JSON_TYPE), CloudVmInstanceStatus[].class);
        return Arrays.asList(vmInstanceStatuses);
    }

    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        LOGGER.info("stop instances on mock spi");
        CloudVmInstanceStatus[] vmInstanceStatuses = mockUrlFactory.get(authenticatedContext, "/spi/stop_instances")
                .post(Entity.entity(vms, MediaType.APPLICATION_JSON_TYPE), CloudVmInstanceStatus[].class);
        return Arrays.asList(vmInstanceStatuses);
    }

    @Override
    public List<CloudVmInstanceStatus> reboot(AuthenticatedContext authenticatedContext, List<CloudInstance> vms) {
        LOGGER.info("reboot instance statuses to mock spi");
        mockUrlFactory.get(authenticatedContext, "/spi/reboot_instances").post(null, String.class);
        return Collections.emptyList();
    }

    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext authenticatedContext, List<CloudInstance> vms) {
        try {
            LOGGER.debug("Collect instance statuses from mock spi");
            CloudVmInstanceStatus[] cloudVmInstanceStatusArray = mockUrlFactory.get(authenticatedContext, "/spi/cloud_instance_statuses")
                    .post(Entity.entity(vms, MediaType.APPLICATION_JSON_TYPE), CloudVmInstanceStatus[].class);
            LOGGER.debug("Collected instance statuses: " + Arrays.toString(cloudVmInstanceStatusArray));
            List<CloudVmInstanceStatus> cloudVmInstanceStatuses = new ArrayList<>();
            for (CloudInstance instance : vms) {
                Optional<CloudVmInstanceStatus> vmInstanceStatusFromApi = Arrays.stream(cloudVmInstanceStatusArray)
                        .filter(instanceStatus -> instanceStatus.getCloudInstance().getTemplate().getPrivateId().equals(instance.getTemplate().getPrivateId()))
                        .findFirst();
                InstanceStatus instanceStatus = InstanceStatus.TERMINATED;
                if (vmInstanceStatusFromApi.isPresent()) {
                    instanceStatus = vmInstanceStatusFromApi.get().getStatus();
                }
                CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(instance, instanceStatus);
                cloudVmInstanceStatuses.add(cloudVmInstanceStatus);
            }
            return cloudVmInstanceStatuses;
        } catch (NotFoundException e) {
            LOGGER.info("Cannot find on the provider with resource id: {}", authenticatedContext.getCloudContext().getName());
            return vms.stream().map(it -> new CloudVmInstanceStatus(it, InstanceStatus.TERMINATED, "Cannot find on the provider")).collect(Collectors.toList());
        }
    }

    @Override
    public String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm) {
        return CB_FINGERPRINT;
    }
}
