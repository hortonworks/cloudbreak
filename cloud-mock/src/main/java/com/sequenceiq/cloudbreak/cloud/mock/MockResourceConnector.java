package com.sequenceiq.cloudbreak.cloud.mock;

import static com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.CREATED;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
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
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class MockResourceConnector implements ResourceConnector<Object> {

    public static final String MOCK_RDS_PORT = "1234";

    public static final String MOCK_RDS_HOST = "mockrdshost";

    public static final int VOLUME_COUNT_PER_MOCK_INSTANCE = 5;

    private static final Logger LOGGER = LoggerFactory.getLogger(MockResourceConnector.class);

    @Inject
    private MockCredentialViewFactory mockCredentialViewFactory;

    @Inject
    private PersistenceNotifier resourceNotifier;

    @Inject
    private CloudResourceHelper cloudResourceHelper;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier,
            AdjustmentType adjustmentType, Long threshold) {
        List<CloudResourceStatus> cloudResourceStatuses = new ArrayList<>();
        Iterator<String> instanceIdIterator = getInstanceIdIterator(authenticatedContext);
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        String instancId = "";
        for (Group group : stack.getGroups()) {
            for (int i = 0; i < group.getInstancesSize(); i++) {
                instancId = getNextInsanceId(instanceIdIterator, instancId);
                CloudResource instanceResource = generateResource("cloudinstance" + cloudResourceStatuses.size(), cloudContext,
                        group.getName(), ResourceType.MOCK_INSTANCE, instancId);
                generateResource("cloudtemplate" + cloudResourceStatuses.size(), cloudContext, group.getName(), ResourceType.MOCK_TEMPLATE, instancId);
                for (int j = 0; j < VOLUME_COUNT_PER_MOCK_INSTANCE; j++) {
                    generateResource("cloudvolume" + cloudResourceStatuses.size(), cloudContext, group.getName(), ResourceType.MOCK_VOLUME, instancId);
                }
                cloudResourceStatuses.add(new CloudResourceStatus(instanceResource, CREATED));
            }
        }
        return cloudResourceStatuses;
    }

    private String getNextInsanceId(Iterator<String> instanceIdIterator, String instancId) {
        if (instanceIdIterator.hasNext()) {
            instancId = instanceIdIterator.next();
        }
        return instancId;
    }

    private Iterator<String> getInstanceIdIterator(AuthenticatedContext authenticatedContext) {
        try {
            MockCredentialView mockCredentialView = mockCredentialViewFactory.createCredetialView(authenticatedContext.getCloudCredential());
            CloudVmInstanceStatus[] cloudVmInstanceStatusArray = Unirest.post(mockCredentialView.getMockEndpoint() + "/spi/cloud_instance_statuses")
                    .asObject(CloudVmInstanceStatus[].class).getBody();
            List<String> instanceIds = Arrays.stream(cloudVmInstanceStatusArray)
                    .filter(instanceStatus -> InstanceStatus.STARTED == instanceStatus.getStatus())
                    .map(CloudVmInstanceStatus::getCloudInstance)
                    .map(CloudInstance::getInstanceId)
                    .sorted(new MockInstanceIdComparator())
                    .collect(Collectors.toList());
            return instanceIds.iterator();
        } catch (UnirestException e) {
            LOGGER.error("Couldn't fetch CloudInstances", e);
            throw new RuntimeException("Couldn't fetch CloudInstances", e);
        }
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
        return emptyList();
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> cloudResources) {
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
    public List<CloudResourceStatus> upscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        List<CloudResourceStatus> cloudResourceStatuses = new ArrayList<>();
        for (CloudResource cloudResource : resources) {
            CloudResourceStatus cloudResourceStatus = new CloudResourceStatus(cloudResource, CREATED);
            cloudResourceStatuses.add(cloudResourceStatus);
        }
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        List<Group> scaledGroups = cloudResourceHelper.getScaledGroups(stack);
        Iterator<String> instanceIdIterator = getInstanceIdIterator(authenticatedContext);
        String instanaceId = "";
        for (Group scaledGroup : scaledGroups) {
            for (int i = 0; i < scaledGroup.getInstances().size(); i++) {
                instanaceId = getNextInsanceId(instanceIdIterator, instanaceId);
                CloudResource instanceResource = generateResource("cloudvolume" + cloudResourceStatuses.size(), cloudContext,
                        scaledGroup.getName(), ResourceType.MOCK_INSTANCE, instanaceId);
                generateResource("cloudvolume" + cloudResourceStatuses.size(), cloudContext, scaledGroup.getName(), ResourceType.MOCK_TEMPLATE,
                        instanaceId);
                for (int j = 0; j < VOLUME_COUNT_PER_MOCK_INSTANCE; j++) {
                    generateResource("cloudvolume" + cloudResourceStatuses.size(), cloudContext, scaledGroup.getName(), ResourceType.MOCK_VOLUME,
                            instanaceId);
                }
                cloudResourceStatuses.add(new CloudResourceStatus(instanceResource, CREATED));
            }
        }

        return cloudResourceStatuses;
    }

    private CloudResource generateResource(String name, CloudContext cloudContext, String group, ResourceType type, String instanceId) {
        String generatedRandom = RandomStringUtils.random(100);
        Map<String, Object> params = new HashMap<>();
        params.put("generated", generatedRandom);
        CloudResource resource = new Builder()
                .type(type)
                .status(CommonStatus.CREATED)
                .group(group)
                .name(name)
                .instanceId(instanceId)
                .params(params)
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
        try {
            MockCredentialView mockCredentialView = mockCredentialViewFactory.createCredetialView(authenticatedContext.getCloudCredential());
            Unirest.post(mockCredentialView.getMockEndpoint() + "/spi/terminate_instances").body(vms).asString();
        } catch (UnirestException e) {
            throw new RuntimeException("rest error", e);
        }
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
