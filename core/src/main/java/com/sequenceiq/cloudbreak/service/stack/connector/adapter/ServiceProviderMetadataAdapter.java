package com.sequenceiq.cloudbreak.service.stack.connector.adapter;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.converter.spi.CloudVmInstanceStatusToCoreInstanceMetaDataConverter;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ServiceProviderMetadataAdapter implements MetadataSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProviderMetadataAdapter.class);

    @Inject
    private EventBus eventBus;
    @Inject
    private StackToCloudStackConverter cloudStackConverter;
    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;
    @Inject
    private CloudVmInstanceStatusToCoreInstanceMetaDataConverter instanceConverter;
    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;
    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Override
    public Set<CoreInstanceMetaData> collectMetadata(Stack stack) {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name(), stack.getOwner(), stack.getPlatformVariant(),
                location);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        List<InstanceTemplate> instanceTemplates = cloudStackConverter.buildInstanceTemplates(stack);
        List<CloudResource> cloudResources = cloudResourceConverter.convert(stack.getResources());
        CollectMetadataRequest<CollectMetadataResult> cmr = new CollectMetadataRequest<>(cloudContext, cloudCredential, cloudResources, instanceTemplates);
        LOGGER.info("Triggering event: {}", cmr);
        eventBus.notify(cmr.selector(CollectMetadataRequest.class), Event.wrap(cmr));
        try {
            CollectMetadataResult res = cmr.await();
            LOGGER.info("Result: {}", res);
            if (res.getException() != null) {
                LOGGER.error("Failed to collect metadata", res.getException());
                return Collections.emptySet();
            }
            return new HashSet<>(instanceConverter.convert(res.getResults()));
        } catch (InterruptedException e) {
            LOGGER.error(format("Error while executing collectMetadata, stack: %s", cloudContext), e);
            throw new OperationException(e);
        }
    }

    @Override
    public Set<CoreInstanceMetaData> collectNewMetadata(Stack stack, Set<Resource> resourceList, String instanceGroupName, Integer scalingAdjustment) {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name(), stack.getOwner(), stack.getPlatformVariant(),
                location);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        List<InstanceTemplate> instanceTemplates = getNewInstanceTemplates(stack);
        List<CloudResource> cloudResources = cloudResourceConverter.convert(stack.getResources());
        CollectMetadataRequest<CollectMetadataResult> cmr = new CollectMetadataRequest<>(cloudContext, cloudCredential, cloudResources, instanceTemplates);
        LOGGER.info("Triggering event: {}", cmr);
        eventBus.notify(cmr.selector(CollectMetadataRequest.class), Event.wrap(cmr));
        try {
            CollectMetadataResult res = cmr.await();
            LOGGER.info("Result: {}", res);
            if (res.getException() != null) {
                LOGGER.error(format("Failed to collect metadata, stack: %s", cloudContext), res.getException());
                return Collections.emptySet();
            }
            return new HashSet<>(instanceConverter.convert(res.getResults()));
        } catch (InterruptedException e) {
            LOGGER.error(format("Error while collecting new metadata for stack: %s", cloudContext), e);
            throw new OperationException(e);
        }
    }

    @Override
    public InstanceSyncState getState(Stack stack, InstanceGroup instanceGroup, String instanceId) {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name(), stack.getOwner(), stack.getPlatformVariant(),
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
                    LOGGER.error("Failed to retrieve instance state", res.getException());
                    throw new OperationException(res.getException());
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

    @Override
    public ResourceType getInstanceResourceType() {
        return null;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.ADAPTER;
    }


    private List<InstanceTemplate> getNewInstanceTemplates(Stack stack) {
        List<InstanceTemplate> instanceTemplates = cloudStackConverter.buildInstanceTemplates(stack);
        Iterator<InstanceTemplate> iterator = instanceTemplates.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getStatus() != InstanceStatus.CREATE_REQUESTED) {
                iterator.remove();
            }
        }
        return instanceTemplates;
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
