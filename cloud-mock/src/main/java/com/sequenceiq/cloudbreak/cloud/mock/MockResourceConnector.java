package com.sequenceiq.cloudbreak.cloud.mock;

import static com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.CREATED;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingDoesNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
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
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        for (Group group : stack.getGroups()) {
            for (int i = 0; i < group.getInstancesSize(); i++) {
                CloudResource instanceResource = generateResource("cloudvolume" + cloudResourceStatuses.size(), cloudContext,
                        group.getName(), ResourceType.MOCK_INSTANCE);
                generateResource("cloudvolume" + cloudResourceStatuses.size(), cloudContext, group.getName(), ResourceType.MOCK_TEMPLATE);
                for (int j = 0; j < VOLUME_COUNT_PER_MOCK_INSTANCE; j++) {
                    generateResource("cloudvolume" + cloudResourceStatuses.size(), cloudContext, group.getName(), ResourceType.MOCK_VOLUME);
                }
                cloudResourceStatuses.add(new CloudResourceStatus(instanceResource, CREATED));
            }
        }
        return cloudResourceStatuses;
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
        return new ArrayList<>();
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> cloudResources) {
        return new ArrayList<>();
    }

    @Override
    public List<CloudResourceStatus> terminateDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack, boolean force) {
        return new ArrayList<>();
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        return new ArrayList<>();
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
        for (Group scaledGroup : scaledGroups) {
            for (int i = 0; i < scaledGroup.getInstances().size(); i++) {
                CloudResource instanceResource = generateResource("cloudvolume" + cloudResourceStatuses.size(), cloudContext,
                        scaledGroup.getName(), ResourceType.MOCK_INSTANCE);
                generateResource("cloudvolume" + cloudResourceStatuses.size(), cloudContext, scaledGroup.getName(), ResourceType.MOCK_TEMPLATE);
                for (int j = 0; j < VOLUME_COUNT_PER_MOCK_INSTANCE; j++) {
                    generateResource("cloudvolume" + cloudResourceStatuses.size(), cloudContext, scaledGroup.getName(), ResourceType.MOCK_VOLUME);
                }
                cloudResourceStatuses.add(new CloudResourceStatus(instanceResource, CREATED));
            }
        }

        return cloudResourceStatuses;
    }

    private CloudResource generateResource(String name, CloudContext cloudContext, String group, ResourceType type) {
        String generatedRandom = new Random().ints(97, 123)
                .limit(100000)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        Map<String, Object> params = new HashMap<>();
        params.put("generated", generatedRandom);
        CloudResource resource = new Builder()
                .type(type)
                .status(CommonStatus.CREATED)
                .group(group)
                .name(name)
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
        return new ArrayList<>();
    }

    @Override
    public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        return new TlsInfo(false);
    }

    @Override
    public String getStackTemplate() throws TemplatingDoesNotSupportedException {
        throw new TemplatingDoesNotSupportedException();
    }

    @Override
    public String getDBStackTemplate() throws TemplatingDoesNotSupportedException {
        //throw new TemplatingDoesNotSupportedException();
        return "BestDbStackTemplateInTheWorld";
    }
}
