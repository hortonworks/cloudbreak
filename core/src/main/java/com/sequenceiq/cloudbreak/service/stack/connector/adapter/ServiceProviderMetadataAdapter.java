package com.sequenceiq.cloudbreak.service.stack.connector.adapter;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;
import com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ServiceProviderMetadataAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProviderMetadataAdapter.class);

    @Inject
    private EventBus eventBus;
    @Inject
    private StackToCloudStackConverter cloudStackConverter;
    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;
    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;
    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    public List<CloudVmMetaDataStatus> collectMetadata(Stack stack) {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getOwner(), stack.getPlatformVariant(),
                location);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        List<CloudInstance> cloudInstances = cloudStackConverter.buildInstances(stack);
        List<CloudResource> cloudResources = cloudResourceConverter.convert(stack.getResources());
        CollectMetadataRequest cmr = new CollectMetadataRequest(cloudContext, cloudCredential, cloudResources, cloudInstances);
        LOGGER.info("Triggering event: {}", cmr);
        eventBus.notify(cmr.selector(CollectMetadataRequest.class), Event.wrap(cmr));
        try {
            CollectMetadataResult res = cmr.await();
            LOGGER.info("Result: {}", res);
            if (res.getException() != null) {
                LOGGER.error("Failed to collect metadata", res.getException());
                return Collections.emptyList();
            }
            return res.getResults();
        } catch (InterruptedException e) {
            LOGGER.error(format("Error while executing collectMetadata, stack: %s", cloudContext), e);
            throw new OperationException(e);
        }
    }

    public List<CloudVmMetaDataStatus> collectNewMetadata(Stack stack) {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getOwner(), stack.getPlatformVariant(),
                location);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        List<CloudInstance> cloudInstances = getNewInstances(stack);
        List<CloudResource> cloudResources = cloudResourceConverter.convert(stack.getResources());
        CollectMetadataRequest cmr = new CollectMetadataRequest(cloudContext, cloudCredential, cloudResources, cloudInstances);
        LOGGER.info("Triggering event: {}", cmr);
        eventBus.notify(cmr.selector(CollectMetadataRequest.class), Event.wrap(cmr));
        try {
            CollectMetadataResult res = cmr.await();
            LOGGER.info("Result: {}", res);
            if (res.getException() != null) {
                LOGGER.error(format("Failed to collect metadata, stack: %s", cloudContext), res.getException());
                return Collections.emptyList();
            }
            return res.getResults();
        } catch (InterruptedException e) {
            LOGGER.error(format("Error while collecting new metadata for stack: %s", cloudContext), e);
            throw new OperationException(e);
        }
    }

    public InstanceSyncState getState(Stack stack, InstanceGroup instanceGroup, String instanceId) {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getOwner(), stack.getPlatformVariant(),
                location);
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
                    new GetInstancesStateRequest<>(cloudContext, cloudCredential, asList(instance));
            LOGGER.info("Triggering event: {}", stateRequest);
            eventBus.notify(stateRequest.selector(), Event.wrap(stateRequest));
            try {
                GetInstancesStateResult res = stateRequest.await();
                LOGGER.info("Result: {}", res);
                if (res.isFailed()) {
                    LOGGER.error("Failed to retrieve instance state", res.getErrorDetails());
                    throw new OperationException(res.getErrorDetails());
                }
                return transform(res.getStatuses().get(0).getStatus());
            } catch (InterruptedException e) {
                LOGGER.error(format("Error while retrieving instance state of: %s", cloudContext), e);
                throw new OperationException(e);
            }
        } else {
            return InstanceSyncState.DELETED;
        }
    }

    private List<CloudInstance> getNewInstances(Stack stack) {
        List<CloudInstance> cloudInstances = cloudStackConverter.buildInstances(stack);
        Iterator<CloudInstance> iterator = cloudInstances.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getTemplate().getStatus() != InstanceStatus.CREATE_REQUESTED) {
                iterator.remove();
            }
        }
        return cloudInstances;
    }

    private InstanceSyncState transform(InstanceStatus instanceStatus) {
        switch (instanceStatus) {
            case IN_PROGRESS:
                return InstanceSyncState.IN_PROGRESS;
            case STARTED:
                return InstanceSyncState.RUNNING;
            case STOPPED:
                return InstanceSyncState.STOPPED;
            case CREATED:
                return InstanceSyncState.RUNNING;
            case FAILED:
                return InstanceSyncState.DELETED;
            case TERMINATED:
                return InstanceSyncState.DELETED;
            default:
                return InstanceSyncState.UNKNOWN;
        }
    }

}
