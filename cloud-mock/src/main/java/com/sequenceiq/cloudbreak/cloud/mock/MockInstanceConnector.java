package com.sequenceiq.cloudbreak.cloud.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
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

    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = new ArrayList<>();
        for (CloudInstance instance : vms) {
            CloudVmInstanceStatus instanceStatus = new CloudVmInstanceStatus(instance, InstanceStatus.CREATED);
            cloudVmInstanceStatuses.add(instanceStatus);
        }

        MockCredentialView mockCredentialView = mockCredentialViewFactory.createCredetialView(authenticatedContext.getCloudCredential());
        LOGGER.info("stop instance statuses to mock spi, server address: " + mockCredentialView.getMockEndpoint());
        try {
            Unirest.post(mockCredentialView.getMockEndpoint() + "/spi/start_instances").asString();
        } catch (UnirestException e) {
            LOGGER.error("Error when instances got started", e);
        }
        return cloudVmInstanceStatuses;
    }

    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = new ArrayList<>();
        for (CloudInstance instance : vms) {
            CloudVmInstanceStatus instanceStatus = new CloudVmInstanceStatus(instance, InstanceStatus.STOPPED);
            cloudVmInstanceStatuses.add(instanceStatus);
        }

        MockCredentialView mockCredentialView = mockCredentialViewFactory.createCredetialView(authenticatedContext.getCloudCredential());
        LOGGER.info("stop instance statuses to mock spi, server address: " + mockCredentialView.getMockEndpoint());
        try {
            Unirest.post(mockCredentialView.getMockEndpoint() + "/spi/stop_instances").asString();
        } catch (UnirestException e) {
            LOGGER.error("Error when instances got stopped", e);
        }

        return cloudVmInstanceStatuses;
    }

    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext authenticatedContext, List<CloudInstance> vms) {

        try {
            MockCredentialView mockCredentialView = mockCredentialViewFactory.createCredetialView(authenticatedContext.getCloudCredential());
            LOGGER.info("collect instance statuses from mock spi, server address: " + mockCredentialView.getMockEndpoint());
            CloudVmInstanceStatus[] cloudVmInstanceStatusArray = Unirest.post(mockCredentialView.getMockEndpoint() + "/spi/cloud_instance_statuses")
                    .asObject(CloudVmInstanceStatus[].class).getBody();
            LOGGER.info("collected instance statuses: " + Arrays.toString(cloudVmInstanceStatusArray));
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
        } catch (UnirestException e) {
            throw new RuntimeException("can't convert to object", e);
        }
    }

    @Override
    public String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm) {
        return CB_FINGERPRINT;
    }
}
