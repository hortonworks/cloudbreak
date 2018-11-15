package com.sequenceiq.cloudbreak.service.stack.connector.adapter;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static java.lang.String.format;

import java.util.Collections;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;
import com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState;

import reactor.bus.EventBus;

@Component
public class ServiceProviderMetadataAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProviderMetadataAdapter.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    public InstanceSyncState getState(Stack stack, InstanceGroup instanceGroup, String instanceId) {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getPlatformVariant(),
                location, stack.getCreator().getUserId(), stack.getWorkspace().getId());
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        InstanceGroup ig = stack.getInstanceGroupByInstanceGroupName(instanceGroup.getGroupName());
        CloudInstance instance = null;
        for (InstanceMetaData metaData : ig.getAllInstanceMetaData()) {
            if (instanceId.equalsIgnoreCase(metaData.getInstanceId())) {
                instance = metadataConverter.convert(metaData);
                break;
            }
        }
        if (instance != null) {
            GetInstancesStateRequest<GetInstancesStateResult> stateRequest =
                    new GetInstancesStateRequest<>(cloudContext, cloudCredential, Collections.singletonList(instance));
            LOGGER.info("Triggering event: {}", stateRequest);
            eventBus.notify(stateRequest.selector(), eventFactory.createEvent(stateRequest));
            try {
                GetInstancesStateResult res = stateRequest.await();
                LOGGER.info("Result: {}", res);
                if (res.isFailed()) {
                    LOGGER.error("Failed to retrieve instance state", res.getErrorDetails());
                    throw new OperationException(res.getErrorDetails());
                }
                return InstanceSyncState.getInstanceSyncState(res.getStatuses().get(0).getStatus());
            } catch (InterruptedException e) {
                LOGGER.error(format("Error while retrieving instance state of: %s", cloudContext), e);
                throw new OperationException(e);
            }
        } else {
            return InstanceSyncState.DELETED;
        }
    }

}
