package com.sequenceiq.cloudbreak.core.flow2.stack.migration.handler;

import static com.sequenceiq.cloudbreak.core.flow2.stack.migration.AwsVariantMigrationEvent.AWS_VARIANT_MIGRATION_FAILED_EVENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsAuthenticator;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsTerminateService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.DeleteCloudFormationRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.DeleteCloudFormationResult;
import com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class DeleteCloudFormationHandler implements CloudPlatformEventHandler<DeleteCloudFormationRequest> {

    private static final Logger LOGGER = getLogger(DeleteCloudFormationHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private AwsTerminateService awsTerminateService;

    @Inject
    private AwsAuthenticator awsAuthenticator;

    @Inject
    private ResourceRetriever resourceRetriever;

    @Inject
    private ResourceNotifier resourceNotifier;

    @Inject
    private AwsMigrationUtil awsMigrationUtil;

    @Override
    public Class<DeleteCloudFormationRequest> type() {
        return DeleteCloudFormationRequest.class;
    }

    @Override
    public void accept(Event<DeleteCloudFormationRequest> event) {
        LOGGER.info("Delete the old cloud formation during the AWS migration");
        DeleteCloudFormationRequest request = event.getData();
        try {
            CloudContext cloudContext = request.getCloudContext();
            CloudCredential cloudCredential = request.getCloudCredential();
            CloudStack cloudStack = request.getCloudStack();
            AuthenticatedContext ac = awsAuthenticator.authenticate(cloudContext, cloudCredential);
            Optional<CloudResource> cfCloudResource = resourceRetriever
                    .findFirstByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.CLOUDFORMATION_STACK, request.getResourceId());
            boolean cloudFormationTemplateDeleted = false;
            if (cfCloudResource.isPresent()) {
                if (awsMigrationUtil.allInstancesDeletedFromCloudFormation(ac, cfCloudResource.get())) {
                    terminateCfStackAndResource(cloudContext, cloudStack, ac, cfCloudResource.get());
                    cloudFormationTemplateDeleted = true;
                } else {
                    LOGGER.info("Cannot delete the Cloudformation because it contains instances");
                }
            } else {
                LOGGER.info("Cloud formation does not exist on AWS side, maybe deleted previously or by the customer");
                cloudFormationTemplateDeleted = true;
            }
            DeleteCloudFormationResult result = new DeleteCloudFormationResult(request.getResourceId(), cloudFormationTemplateDeleted);
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
        } catch (Exception e) {
            LOGGER.error("Cannot delete the AWS cloud formation template during the variant migration", e);
            DeleteCloudFormationResult result = new DeleteCloudFormationResult(e.getMessage(), e, request.getResourceId());
            request.getResult().onNext(result);
            eventBus.notify(AWS_VARIANT_MIGRATION_FAILED_EVENT.event(), new Event<>(event.getHeaders(), result));
        }
    }

    private void terminateCfStackAndResource(CloudContext cloudContext, CloudStack cloudStack, AuthenticatedContext ac,
            CloudResource cfCloudResource) {
        List<CloudResourceStatus> terminateResult = awsTerminateService.terminate(ac, cloudStack, List.of(cfCloudResource));
        ResourcePersisted resourceDeletionResult = resourceNotifier.notifyDeletion(cfCloudResource, cloudContext);
        LOGGER.debug("resource deletion result: {}", resourceDeletionResult);
        LOGGER.info("Cloud formation terminated: {}", terminateResult);
    }
}
