package com.sequenceiq.cloudbreak.core.flow2.stack.migration.handler;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.core.flow2.stack.migration.AwsVariantMigrationEvent.AWS_VARIANT_MIGRATION_FAILED_EVENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsAuthenticator;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContextBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.resource.group.AwsSecurityGroupResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.CreateResourcesRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.CreateResourcesResult;
import com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CreateResourcesHandler implements CloudPlatformEventHandler<CreateResourcesRequest> {

    private static final Logger LOGGER = getLogger(CreateResourcesHandler.class);

    @Inject
    private AwsSecurityGroupResourceBuilder awsSecurityGroupResourceBuilder;

    @Inject
    private AwsContextBuilder awsContextBuilder;

    @Inject
    private AwsAuthenticator awsAuthenticator;

    @Inject
    private EventBus eventBus;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Override
    public Class<CreateResourcesRequest> type() {
        return CreateResourcesRequest.class;
    }

    @Override
    public void accept(Event<CreateResourcesRequest> event) {
        LOGGER.info("Re-create the resources during the AWS migration");
        CreateResourcesRequest request = event.getData();
        try {
            CloudContext cloudContext = request.getCloudContext();
            CloudCredential cloudCredential = request.getCloudCredential();
            CloudStack cloudStack = request.getCloudStack();
            AuthenticatedContext ac = awsAuthenticator.authenticate(cloudContext, cloudCredential);
            Network network = cloudStack.getNetwork();
            AwsContext awsContext = awsContextBuilder.contextInit(cloudContext, ac, network, List.of(), true);
            Group group = getGroupByName(cloudStack.getGroups(), request.getHostGroupName());
            CloudResource createdSecurityGroup = awsSecurityGroupResourceBuilder.create(awsContext, ac, group, network);
            createdSecurityGroup = CloudResource.builder().cloudResource(createdSecurityGroup).group(group.getName()).build();
            ResourcePersisted resourcePersisted = persistenceNotifier.notifyAllocation(createdSecurityGroup, cloudContext);
            if (resourcePersisted.getException() != null) {
                LOGGER.error("Cannot create security group resource in the db: {}", resourcePersisted.getStatusReason(), resourcePersisted.getException());
                throw resourcePersisted.getException();
            }
            CloudResource existedSecGroup = awsSecurityGroupResourceBuilder.build(awsContext, ac, group, network, group.getSecurity(), createdSecurityGroup);
            persistenceNotifier.notifyUpdate(existedSecGroup, cloudContext);
            LOGGER.info("AWS security group successfully created");
            CreateResourcesResult result = new CreateResourcesResult(request.getResourceId());
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
        } catch (Exception e) {
            LOGGER.error("Cannot re-create the AWS security group during the variant migration", e);
            CreateResourcesResult result = new CreateResourcesResult(e.getMessage(), e, request.getResourceId());
            request.getResult().onNext(result);
            eventBus.notify(AWS_VARIANT_MIGRATION_FAILED_EVENT.event(), new Event<>(event.getHeaders(), result));
        }
    }

    private Group getGroupByName(List<Group> groups, String hostGroupName) {
        return groups.stream().filter(g -> hostGroupName.equals(g.getName())).findFirst().orElseThrow(notFound("hostGroup", hostGroupName));
    }
}
