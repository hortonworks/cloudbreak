package com.sequenceiq.cloudbreak.cloud.mock;

import static com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.CREATED;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class MockResourceConnector implements ResourceConnector<Object> {

    public static final String MOCK_RDS_PORT = "1234";

    public static final String MOCK_RDS_HOST = "mockrdshost";

    private static final Logger LOGGER = LoggerFactory.getLogger(MockResourceConnector.class);

    @Inject
    private PersistenceNotifier resourceNotifier;

    @Inject
    private CloudResourceHelper cloudResourceHelper;

    @Inject
    private MockUrlFactory mockUrlFactory;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) {
        List<CloudVmInstanceStatus> instanceIdIterator = launch(authenticatedContext, stack);
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        return generateResources(cloudContext, instanceIdIterator);
    }

    private List<CloudVmInstanceStatus> launch(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        CloudVmInstanceStatus[] cloudVmInstanceStatusArray = mockUrlFactory.get(authenticatedContext, "/spi/launch")
                .post(Entity.entity(cloudStack, MediaType.APPLICATION_JSON_TYPE), CloudVmInstanceStatus[].class);
        return Arrays.asList(cloudVmInstanceStatusArray);
    }

    private List<CloudVmInstanceStatus> resize(AuthenticatedContext authenticatedContext, List<Group> groups) {
        CloudVmInstanceStatus[] cloudVmInstanceStatusArray = mockUrlFactory.get(authenticatedContext, "/spi/add_instance")
                .post(Entity.entity(groups, MediaType.APPLICATION_JSON_TYPE), CloudVmInstanceStatus[].class);
        return Arrays.asList(cloudVmInstanceStatusArray);
    }

    @Override
    public List<CloudResourceStatus> launchLoadBalancers(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier)
            throws Exception {
        throw new UnsupportedOperationException("Load balancers are not supported for the mock resource connector.");
    }

    @Override
    public List<CloudResourceStatus> launchDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack,
            PersistenceNotifier persistenceNotifier) {
        List<CloudResource> cloudResources = List.of(
                new Builder()
                        .type(ResourceType.RDS_HOSTNAME)
                        .status(CommonStatus.CREATED)
                        .name(MOCK_RDS_HOST)
                        .persistent(true)
                        .build(),
                new Builder()
                        .type(ResourceType.RDS_PORT)
                        .status(CommonStatus.CREATED)
                        .name(MOCK_RDS_PORT)
                        .persistent(true)
                        .build()
        );
        cloudResources.forEach(cr -> persistenceNotifier.notifyAllocation(cr, authenticatedContext.getCloudContext()));
        return cloudResources.stream()
                .map(cr -> new CloudResourceStatus(cr, CREATED))
                .collect(Collectors.toList());
    }

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        CloudVmInstanceStatus[] post = mockUrlFactory.get(authenticatedContext, "/spi/cloud_instance_statuses")
                .post(Entity.entity(resources, MediaType.APPLICATION_JSON_TYPE), CloudVmInstanceStatus[].class);
        return generateResources(authenticatedContext.getCloudContext(), Arrays.asList(post));
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> cloudResources) {
        try (Response ignore = mockUrlFactory.get(authenticatedContext, "/spi/terminate").delete()) {
        }
        return emptyList();
    }

    @Override
    public List<CloudResourceStatus> terminateDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack,
            List<CloudResource> resources, PersistenceNotifier persistenceNotifier, boolean force) {
        return emptyList();
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        return emptyList();
    }

    @Override
    public void updateUserData(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources, String userData) {

    }

    @Override
    public void checkUpdate(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) throws Exception {
        return;
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) {
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        List<Group> scaledGroups = cloudResourceHelper.getScaledGroups(stack);
        List<Group> groups = scaledGroups.stream()
                .map(g -> {
                    List<CloudInstance> newInstances = g.getInstances().stream()
                            .filter(i -> i.getTemplate().getStatus() == InstanceStatus.CREATE_REQUESTED)
                            .collect(Collectors.toList());
                    return new Group(g.getName(),
                            g.getType(),
                            newInstances,
                            g.getSecurity(),
                            g.getReferenceInstanceConfiguration(),
                            g.getInstanceAuthentication(),
                            g.getLoginUserName(),
                            g.getPublicKey(),
                            g.getRootVolumeSize(),
                            g.getIdentity(),
                            g.getNetwork(),
                            g.getTags());
                }).collect(Collectors.toList());

        List<CloudVmInstanceStatus> resized = resize(authenticatedContext, groups);
        return generateResources(cloudContext, resized);
    }

    private List<CloudResourceStatus> generateResources(CloudContext cloudContext, List<CloudVmInstanceStatus> resize) {
        List<CloudResourceStatus> ret = new ArrayList<>();
        for (CloudVmInstanceStatus cloudVmInstanceStatus : resize) {
            CloudInstance cloudInstance = cloudVmInstanceStatus.getCloudInstance();
            CloudResource instanceResource = generateResource("cloudinstance" + ret.size(), cloudContext, cloudInstance, cloudInstance.getInstanceId(),
                    ResourceType.MOCK_INSTANCE);
            List<Volume> volumes = cloudInstance.getTemplate().getVolumes();
            for (int i = 0; i < volumes.size(); i++) {
                UUID uuid = UUID.randomUUID();
                String volumeId = cloudInstance.getInstanceId() + "_" + uuid;
                CloudResource volumeResource = generateResource("cloudvolume" + uuid, cloudContext, cloudInstance, volumeId,
                        ResourceType.MOCK_VOLUME);
                ret.add(new CloudResourceStatus(volumeResource, CREATED, cloudInstance.getTemplate().getPrivateId()));
            }
            ret.add(new CloudResourceStatus(instanceResource, CREATED, cloudInstance.getTemplate().getPrivateId()));
        }
        return ret;
    }

    private CloudResource generateResource(String name, CloudContext cloudContext, CloudInstance cloudInstance, String instanceId, ResourceType type) {
        CloudResource resource = new Builder()
                .type(type)
                .status(CommonStatus.CREATED)
                .group(cloudInstance.getTemplate().getGroupName())
                .name(name)
                .instanceId(instanceId)
                .params(cloudInstance.getParameters())
                .persistent(true)
                .build();
        resourceNotifier.notifyAllocation(resource, cloudContext);
        return resource;
    }

    @Override
    public Object collectResourcesToRemove(AuthenticatedContext authenticatedContext, CloudStack stack,
            List<CloudResource> resources, List<CloudInstance> vms) {
        return null;
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            List<CloudInstance> vms, Object resourcesToRemove) {
        mockUrlFactory.get(authenticatedContext, "/spi/terminate_instances").post(Entity.entity(vms, MediaType.APPLICATION_JSON_TYPE), String.class);
        return emptyList();
    }

    @Override
    public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        return new TlsInfo(false);
    }

    @Override
    public String getStackTemplate() throws TemplatingNotSupportedException {
        throw new TemplatingNotSupportedException();
    }

    @Override
    public String getDBStackTemplate() throws TemplatingNotSupportedException {
        //throw new TemplatingDoesNotSupportedException();
        return "BestDbStackTemplateInTheWorld";
    }

    @Override
    public void startDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack) {
        throw new UnsupportedOperationException("Database server start operation is not supported for " + getClass().getName());
    }

    @Override
    public void stopDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack) {
        throw new UnsupportedOperationException("Database server stop operation is not supported for " + getClass().getName());
    }

    @Override
    public ExternalDatabaseStatus getDatabaseServerStatus(AuthenticatedContext authenticatedContext, DatabaseStack stack) throws Exception {
        throw new UnsupportedOperationException("Database server status lookup is not supported for " + getClass().getName());
    }
}
