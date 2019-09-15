package com.sequenceiq.cloudbreak.cloud.mock;

import static com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.CREATED;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
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
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class MockResourceConnector implements ResourceConnector<Object> {

    public static final String MOCK_RDS_PORT = "1234";

    public static final String MOCK_RDS_HOST = "mockrdshost";

    @Inject
    private MockCredentialViewFactory mockCredentialViewFactory;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier,
            AdjustmentType adjustmentType, Long threshold) {
        List<CloudResourceStatus> cloudResourceStatuses = new ArrayList<>();
        for (Group group : stack.getGroups()) {
            for (int i = 0; i < group.getInstancesSize(); i++) {
                CloudResource cloudResource = new Builder()
                        .type(ResourceType.MOCK_INSTANCE)
                        .status(CommonStatus.CREATED)
                        .name("cloudinstance" + cloudResourceStatuses.size())
                        .reference("")
                        .persistent(true)
                        .build();
                cloudResourceStatuses.add(new CloudResourceStatus(cloudResource, CREATED));
            }
        }
        return cloudResourceStatuses;
    }

    @Override
    public List<CloudResourceStatus> launchDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack,
            PersistenceNotifier persistenceNotifier) {
        CloudResourceStatus rdshost = new CloudResourceStatus(
                new Builder()
                        .type(ResourceType.RDS_HOSTNAME)
                        .status(CommonStatus.CREATED)
                        .name(MOCK_RDS_HOST)
                        .persistent(true)
                        .build(),
                CREATED);
        CloudResourceStatus rdsport = new CloudResourceStatus(
                new Builder()
                        .type(ResourceType.RDS_PORT)
                        .status(CommonStatus.CREATED)
                        .name(MOCK_RDS_PORT)
                        .persistent(true)
                        .build(),
                CREATED);
        List<CloudResourceStatus> cloudResourceStatuses = List.of(rdshost, rdsport);
        // for (Group group : stack.getGroups()) {
        //     for (int i = 0; i < group.getInstancesSize(); i++) {
        //         CloudResource cloudResource = new Builder()
        //                 .type(ResourceType.MOCK_INSTANCE)
        //                 .status(CommonStatus.CREATED)
        //                 .name("cloudinstance" + cloudResourceStatuses.size())
        //                 .reference("")
        //                 .persistent(true)
        //                 .build();
        //         cloudResourceStatuses.add(new CloudResourceStatus(cloudResource, CREATED));
        //     }
        // }
        return cloudResourceStatuses;
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
        int createResourceCount = 0;
        for (int i = 0; i < stack.getGroups().size(); i++) {
            createResourceCount += stack.getGroups().get(i).getInstancesSize();
        }
        createResourceCount -= resources.size();
        if (createResourceCount > 0) {
            for (int i = 0; i < createResourceCount; i++) {
                CloudResource cloudResource = new Builder()
                        .type(ResourceType.MOCK_INSTANCE)
                        .status(CommonStatus.CREATED)
                        .name("cloudinstance" + cloudResourceStatuses.size())
                        .reference("")
                        .persistent(true)
                        .build();

                cloudResourceStatuses.add(new CloudResourceStatus(cloudResource, CREATED));
            }
        }

        return cloudResourceStatuses;
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
