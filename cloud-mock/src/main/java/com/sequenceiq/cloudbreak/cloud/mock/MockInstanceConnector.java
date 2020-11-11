package com.sequenceiq.cloudbreak.cloud.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
    private MockCredentialViewFactory mockCredentialViewFactory;

    @Inject
    private MockUrlFactory mockUrlFactory;

    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = new ArrayList<>();
        for (CloudInstance instance : vms) {
            CloudVmInstanceStatus instanceStatus = getVmInstanceStatus(authenticatedContext, instance, "start");
            cloudVmInstanceStatuses.add(instanceStatus);
        }

        MockCredentialView mockCredentialView = mockCredentialViewFactory.createCredetialView(authenticatedContext.getCloudCredential());
        LOGGER.info("start instance statuses to mock spi, server address: " + mockCredentialView.getMockEndpoint());
        try {
            mockUrlFactory.get("/spi/start_instances").post(Entity.entity(null, MediaType.APPLICATION_JSON_TYPE), String.class);
        } catch (Exception e) {
            LOGGER.error("Error when instances got started", e);
        }
        return cloudVmInstanceStatuses;
    }

    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = new ArrayList<>();
        for (CloudInstance instance : vms) {
            CloudVmInstanceStatus instanceStatus = getVmInstanceStatus(authenticatedContext, instance, "stop");
            cloudVmInstanceStatuses.add(instanceStatus);
        }

        MockCredentialView mockCredentialView = mockCredentialViewFactory.createCredetialView(authenticatedContext.getCloudCredential());
        LOGGER.info("stop instance statuses to mock spi, server address: " + mockCredentialView.getMockEndpoint());
        try {
            mockUrlFactory.get("/spi/stop_instances").post(null, String.class);
        } catch (Exception e) {
            LOGGER.error("Error when instances got stopped", e);
        }

        return cloudVmInstanceStatuses;
    }

    @Override
    public List<CloudVmInstanceStatus> reboot(AuthenticatedContext authenticatedContext, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = new ArrayList<>();
        for (CloudInstance instance : vms) {
            CloudVmInstanceStatus instanceStatus = getVmInstanceStatus(authenticatedContext, instance, "start");
            cloudVmInstanceStatuses.add(instanceStatus);
        }

        MockCredentialView mockCredentialView = mockCredentialViewFactory.createCredetialView(authenticatedContext.getCloudCredential());
        LOGGER.info("start instance statuses to mock spi, server address: " + mockCredentialView.getMockEndpoint());
        try {
            mockUrlFactory.get("/spi/reboot_instances").post(null, String.class);
        } catch (Exception e) {
            LOGGER.error("Error when instances got started", e);
        }
        return cloudVmInstanceStatuses;
    }

    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext authenticatedContext, List<CloudInstance> vms) {

        try {
            MockCredentialView mockCredentialView = mockCredentialViewFactory.createCredetialView(authenticatedContext.getCloudCredential());
            LOGGER.debug("Collect instance statuses from mock spi, server address: " + mockCredentialView.getMockEndpoint());
            CloudVmInstanceStatus[] cloudVmInstanceStatusArray = mockUrlFactory.get("/spi/cloud_instance_statuses")
                    .post(null, CloudVmInstanceStatus[].class);
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
        } catch (Exception e) {
            throw new RuntimeException("can't convert to object", e);
        }
    }

    @Override
    public String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm) {
        return CB_FINGERPRINT;
    }

    private CloudVmInstanceStatus getVmInstanceStatus(AuthenticatedContext authenticatedContext, CloudInstance instance, String operation) {
        try {
            Response response = mockUrlFactory.get("spi/" + instance.getInstanceId() + "/" + operation).get();
            if (response.getStatus() != HttpStatus.OK.value()) {
                throw new RuntimeException(operation + "'s http status should be OK but got " + response.getStatus());
            }
            return response.readEntity(CloudVmInstanceStatus.class);
        } catch (Exception e) {
            throw new RuntimeException("can't fetch vm status from mock provider by " + operation, e);
        }
    }
}
