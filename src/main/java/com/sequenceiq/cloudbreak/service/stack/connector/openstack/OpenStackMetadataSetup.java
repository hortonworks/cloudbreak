package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.event.MetadataSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.event.MetadataUpdateComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;

import reactor.core.Reactor;
import reactor.event.Event;

@Component
public class OpenStackMetadataSetup implements MetadataSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackMetadataSetup.class);

    @Autowired
    private Reactor reactor;

    @Autowired
    private OpenStackUtil openStackUtil;

    @Override
    public void setupMetadata(Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        OSClient osClient = openStackUtil.createOSClient(stack);
        Resource heatResource = stack.getResourceByType(ResourceType.HEAT_STACK);
        String heatStackId = heatResource.getResourceName();
        Set<CoreInstanceMetaData> instancesCoreMetadata = new HashSet<>();
        org.openstack4j.model.heat.Stack heatStack = osClient.heat().stacks().getDetails(stack.getName(), heatStackId);
        List<Map<String, Object>> outputs = heatStack.getOutputs();
        for (Map<String, Object> map : outputs) {
            String instanceUUID = (String) map.get("output_value");
            Server server = osClient.compute().servers().get(instanceUUID);
            Map<String, String> metadata = server.getMetadata();
            String instanceGroupName = metadata.get(HeatTemplateBuilder.CB_INSTANCE_GROUP_NAME);
            instancesCoreMetadata.add(createCoreMetaData(stack, server, instanceGroupName, openStackUtil.getInstanceId(instanceUUID, metadata)));
        }
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.METADATA_SETUP_COMPLETE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.METADATA_SETUP_COMPLETE_EVENT,
                Event.wrap(new MetadataSetupComplete(CloudPlatform.OPENSTACK, stack.getId(), instancesCoreMetadata)));
    }

    @Override
    public void addNewNodesToMetadata(Stack stack, Set<Resource> resourceList, final String hostGroup) {
        MDCBuilder.buildMdcContext(stack);
        OSClient osClient = openStackUtil.createOSClient(stack);
        Resource heatResource = stack.getResourceByType(ResourceType.HEAT_STACK);
        String heatStackId = heatResource.getResourceName();
        org.openstack4j.model.heat.Stack heatStack = osClient.heat().stacks().getDetails(stack.getName(), heatStackId);
        List<Map<String, Object>> outputs = heatStack.getOutputs();
        Set<CoreInstanceMetaData> instancesCoreMetadata = new HashSet<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            if (instanceGroup.getGroupName().equals(hostGroup)) {
                for (Map<String, Object> map : outputs) {
                    String instanceUUID = (String) map.get("output_value");
                    Server server = osClient.compute().servers().get(instanceUUID);
                    Map<String, String> metadata = server.getMetadata();
                    String instanceGroupName = metadata.get(HeatTemplateBuilder.CB_INSTANCE_GROUP_NAME);
                    final String instanceId = openStackUtil.getInstanceId(instanceUUID, metadata);
                    boolean metadataExists = FluentIterable.from(instanceGroup.getInstanceMetaData()).anyMatch(new Predicate<InstanceMetaData>() {
                        @Override
                        public boolean apply(InstanceMetaData input) {
                            return input.getInstanceId().equals(instanceId);
                        }
                    });
                    if (!metadataExists && instanceGroupName.equals(hostGroup)) {
                        LOGGER.info("New instance added to metadata: [stack: '{}', instanceId: '{}']", stack.getId(), instanceId);
                        instancesCoreMetadata.add(createCoreMetaData(stack, server, instanceGroupName, instanceId));
                    }
                }
            }
        }
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.METADATA_UPDATE_COMPLETE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.METADATA_UPDATE_COMPLETE_EVENT,
                Event.wrap(new MetadataUpdateComplete(CloudPlatform.OPENSTACK, stack.getId(), instancesCoreMetadata, hostGroup)));
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.OPENSTACK;
    }

    private CoreInstanceMetaData createCoreMetaData(Stack stack, Server server, String instanceGroupName, String instanceId) {
        return new CoreInstanceMetaData(
                instanceId,
                server.getAddresses().getAddresses("app_network").get(0).getAddr(),
                server.getAddresses().getAddresses("app_network").get(1).getAddr(),
                server.getOsExtendedVolumesAttached().size(),
                server.getName(),
                stack.getInstanceGroupByInstanceGroupName(instanceGroupName)
        );
    }
}
