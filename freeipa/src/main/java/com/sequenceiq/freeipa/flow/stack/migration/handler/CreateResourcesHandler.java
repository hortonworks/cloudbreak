package com.sequenceiq.freeipa.flow.stack.migration.handler;

import static com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationEvent.AWS_VARIANT_MIGRATION_FAILED_EVENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsAuthenticator;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContextBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.CreateResourcesRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.CreateResourcesResult;
import com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.freeipa.flow.stack.migration.handler.service.ResourceRecreator;

@Component
public class CreateResourcesHandler implements CloudPlatformEventHandler<CreateResourcesRequest> {

    private static final Logger LOGGER = getLogger(CreateResourcesHandler.class);

    @Inject
    private AwsContextBuilder awsContextBuilder;

    @Inject
    private AwsAuthenticator awsAuthenticator;

    @Inject
    private EventBus eventBus;

    @Inject
    private List<ResourceRecreator> resourceRecreators;

    @Override
    public Class<CreateResourcesRequest> type() {
        return CreateResourcesRequest.class;
    }

    @Override
    public void accept(Event<CreateResourcesRequest> event) {
        LOGGER.info("Re-create the resources during the AWS migration, event: {}", event);
        CreateResourcesRequest request = event.getData();
        try {
            CloudContext cloudContext = request.getCloudContext();
            CloudCredential cloudCredential = request.getCloudCredential();
            CloudStack cloudStack = request.getCloudStack();
            AuthenticatedContext ac = awsAuthenticator.authenticate(cloudContext, cloudCredential);
            Network network = cloudStack.getNetwork();
            AwsContext awsContext = awsContextBuilder.contextInit(cloudContext, ac, network, true);
            for (ResourceRecreator resourceRecreator : resourceRecreators) {
                resourceRecreator.recreate(request, awsContext, ac);
            }
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
}
