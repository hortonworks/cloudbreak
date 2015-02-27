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

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.event.MetadataSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionEvent;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;

@Component
public class OpenStackMetadataSetup implements MetadataSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackMetadataSetup.class);

    @Autowired
    private OpenStackUtil openStackUtil;

    @Override
    public ProvisionEvent setupMetadata(Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        OSClient osClient = openStackUtil.createOSClient(stack);
        Resource heatResource = stack.getResourceByType(ResourceType.HEAT_STACK);
        String heatStackId = heatResource.getResourceName();
        Set<CoreInstanceMetaData> instancesCoreMetadata = new HashSet<>();
        org.openstack4j.model.heat.Stack heatStack = osClient.heat().stacks().getDetails(stack.getName(), heatStackId);
        List<Map<String, Object>> outputs = heatStack.getOutputs();
        for (Map<String, Object> map : outputs) {
            String instanceId = (String) map.get("output_value");
            Server server = osClient.compute().servers().get(instanceId);
            instancesCoreMetadata.add(new CoreInstanceMetaData(
                    instanceId,
                    server.getAddresses().getAddresses("app_network").get(0).getAddr(),
                    server.getAddresses().getAddresses("app_network").get(1).getAddr(),
                    server.getOsExtendedVolumesAttached().size(),
                    server.getName(),
                    stack.getInstanceGroupByInstanceGroupName(server.getMetadata().get(HeatTemplateBuilder.CB_INSTANCE_GROUP_NAME))
            ));
        }
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.METADATA_SETUP_COMPLETE_EVENT, stack.getId());
        return new MetadataSetupComplete(CloudPlatform.OPENSTACK, stack.getId(), instancesCoreMetadata);
    }

    @Override
    public ProvisionEvent addNewNodesToMetadata(Stack stack, Set<Resource> resourceList, String hostGroup) {
        return null;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.OPENSTACK;
    }
}
