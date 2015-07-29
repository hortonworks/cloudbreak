package com.sequenceiq.cloudbreak.service.stack.connector.openstack.adapter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.converter.spi.CloudVmInstanceStatusToCoreInstanceMetaDataConverter;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.connector.openstack.OpenStackMetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.rx.Promise;
import reactor.rx.Promises;

@Component
public class OpenStackMetadataSetupAdapter implements MetadataSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackMetadataSetupAdapter.class);

    @Value("${cb.openstack.experimental.connector:false}")
    private boolean experimentalConnector;

    @Inject
    private OpenStackMetadataSetup openStackMetadataSetup;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CloudVmInstanceStatusToCoreInstanceMetaDataConverter instanceConverter;

    @Inject
    private EventBus eventBus;


    @Override
    public Set<CoreInstanceMetaData> collectMetadata(Stack stack) {
        if (experimentalConnector) {
            CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), CloudPlatform.OPENSTACK.name());
            CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
            List<InstanceTemplate> instanceTemplates = cloudStackConverter.buildInstanceTemplates(stack);
            List<CloudResource> cloudResources = cloudResourceConverter.convert(stack.getResources());
            Promise<CollectMetadataResult> promise = Promises.prepare();
            CollectMetadataRequest cmr = new CollectMetadataRequest(cloudContext, cloudCredential, cloudResources, instanceTemplates, promise);
            LOGGER.info("Triggering event: {}", cmr);
            eventBus.notify(cmr.selector(CollectMetadataRequest.class), Event.wrap(cmr));
            CollectMetadataResult res;
            try {
                res = promise.await(1, TimeUnit.HOURS);
                LOGGER.info("Result: {}", res);
                return new HashSet<>(instanceConverter.convert(res.getResults()));
            } catch (InterruptedException e) {
                LOGGER.error("Error while executing pre-provision check", e);
                throw new RuntimeException("Failed to collect metadata");

            }


        }
        return openStackMetadataSetup.collectMetadata(stack);

    }

    @Override
    public Set<CoreInstanceMetaData> collectNewMetadata(Stack stack, Set<Resource> resourceList, final String instanceGroupName) {
        if (experimentalConnector) {
            return openStackMetadataSetup.collectNewMetadata(stack, instanceGroupName);
        } else {
            return openStackMetadataSetup.collectNewMetadata(stack, instanceGroupName);
        }
    }

    @Override
    public InstanceSyncState getState(Stack stack, InstanceGroup instanceGroup, String instanceId) {
        return openStackMetadataSetup.getState(stack, instanceGroup, instanceId);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.OPENSTACK;
    }

    @Override
    public ResourceType getInstanceResourceType() {
        return openStackMetadataSetup.getInstanceResourceType();
    }

}

