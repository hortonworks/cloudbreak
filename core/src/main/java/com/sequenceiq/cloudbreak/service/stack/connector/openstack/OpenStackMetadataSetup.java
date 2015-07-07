package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import static com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState.DELETED;
import static com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState.IN_PROGRESS;
import static com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState.RUNNING;
import static com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState.STOPPED;
import static com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState.UNKNOWN;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.Server.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState;

@Component
public class OpenStackMetadataSetup implements MetadataSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackMetadataSetup.class);

    @Inject
    private OpenStackUtil openStackUtil;

    @Override
    public Set<CoreInstanceMetaData> collectMetadata(Stack stack) {
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
        return instancesCoreMetadata;
    }

    @Override
    public Set<CoreInstanceMetaData> collectNewMetadata(Stack stack, Set<Resource> resourceList, final String instanceGroupName) {
        OSClient osClient = openStackUtil.createOSClient(stack);
        Resource heatResource = stack.getResourceByType(ResourceType.HEAT_STACK);
        String heatStackId = heatResource.getResourceName();
        org.openstack4j.model.heat.Stack heatStack = osClient.heat().stacks().getDetails(stack.getName(), heatStackId);
        List<Map<String, Object>> outputs = heatStack.getOutputs();
        Set<CoreInstanceMetaData> instancesCoreMetadata = new HashSet<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            if (instanceGroup.getGroupName().equals(instanceGroupName)) {
                for (Map<String, Object> map : outputs) {
                    String instanceUUID = (String) map.get("output_value");
                    Server server = osClient.compute().servers().get(instanceUUID);
                    Map<String, String> metadata = server.getMetadata();
                    String groupName = metadata.get(HeatTemplateBuilder.CB_INSTANCE_GROUP_NAME);
                    final String instanceId = openStackUtil.getInstanceId(instanceUUID, metadata);
                    boolean metadataExists = FluentIterable.from(instanceGroup.getInstanceMetaData()).anyMatch(new Predicate<InstanceMetaData>() {
                        @Override
                        public boolean apply(InstanceMetaData input) {
                            return input.getInstanceId().equals(instanceId);
                        }
                    });
                    if (!metadataExists && groupName.equals(instanceGroupName)) {
                        LOGGER.info("New instance added to metadata: [stack: '{}', instanceId: '{}']", stack.getId(), instanceId);
                        instancesCoreMetadata.add(createCoreMetaData(stack, server, groupName, instanceId));
                    }
                }
            }
        }
        return instancesCoreMetadata;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.OPENSTACK;
    }

    @Override
    public ResourceType getInstanceResourceType() {
        return null;
    }

    private CoreInstanceMetaData createCoreMetaData(Stack stack, Server server, String instanceGroupName, String instanceId) {

        String privateIp = null;
        String floatingIp = null;

        Map<String, List<? extends Address>> adrMap = server.getAddresses().getAddresses();
        LOGGER.debug("Address map: {} of instance: {}", adrMap, server.getName());
        for (String key : adrMap.keySet()) {
            LOGGER.debug("Network resource key: {} of instance: {}", key, server.getName());
            List<? extends Address> adrList = adrMap.get(key);
            for (Address adr : adrList) {
                LOGGER.debug("Network resource key: {} of instance: {}, address: {}", key, adr);
                switch (adr.getType()) {
                    case "fixed":
                        privateIp = adr.getAddr();
                        LOGGER.info("PrivateIp of instance: {} is {}", server.getName(), server.getName(), privateIp);
                        break;
                    case "floating":
                        floatingIp = adr.getAddr();
                        LOGGER.info("FloatingIp of instance: {} is {}", server.getName(), floatingIp);
                        break;
                    default:
                        LOGGER.error("No such network resource type: {}, instance: {}", adr.getType(), server.getName());
                }
            }
        }

        CoreInstanceMetaData md = new CoreInstanceMetaData(
                instanceId,
                privateIp,
                floatingIp,
                server.getOsExtendedVolumesAttached().size(),
                stack.getInstanceGroupByInstanceGroupName(instanceGroupName)
        );

        return md;
    }

    @Override
    public InstanceSyncState getState(Stack stack, InstanceGroup instanceGroup, String instanceId) {
        try {
            InstanceSyncState result;
            OSClient osClient = openStackUtil.createOSClient(stack);
            Resource heatResource = stack.getResourceByType(ResourceType.HEAT_STACK);
            String heatStackId = heatResource.getResourceName();
            org.openstack4j.model.heat.Stack heatStack = osClient.heat().stacks().getDetails(stack.getName(), heatStackId);
            if (heatStack != null) {
                List<Map<String, Object>> outputs = heatStack.getOutputs();
                result = getInstanceSyncState(instanceId, osClient, outputs);
            } else {
                result = DELETED;
            }
            return result;
        } catch (Exception ex) {
            throw new OpenStackResourceException("Failed to retrieve state of instance " + instanceId, ex);
        }
    }

    private InstanceSyncState getInstanceSyncState(String instanceId, OSClient osClient, List<Map<String, Object>> outputs) {
        boolean contains = false;
        InstanceSyncState instanceSyncState = IN_PROGRESS;
        for (Map<String, Object> map : outputs) {
            String instanceUUID = (String) map.get("output_value");
            Server server = osClient.compute().servers().get(instanceUUID);
            Map<String, String> metadata = server.getMetadata();
            if (instanceId.equals(openStackUtil.getInstanceId(instanceUUID, metadata))) {
                if (isActualStatusDesired(server.getStatus(), Status.ACTIVE)) {
                    instanceSyncState = RUNNING;
                    contains = true;
                    break;
                } else if (isActualStatusDesired(server.getStatus(), Status.STOPPED, Status.SUSPENDED, Status.PAUSED)) {
                    instanceSyncState = STOPPED;
                    contains = true;
                    break;
                } else if (isActualStatusDesired(server.getStatus(), Status.DELETED, Status.ERROR, Status.SHUTOFF)) {
                    instanceSyncState = DELETED;
                    contains = true;
                    break;
                } else if (isActualStatusDesired(server.getStatus(), Status.UNKNOWN, Status.UNRECOGNIZED)) {
                    instanceSyncState = UNKNOWN;
                    contains = true;
                    break;
                } else {
                    instanceSyncState = IN_PROGRESS;
                    contains = true;
                    break;
                }
            }
        }
        if (!contains) {
            instanceSyncState = DELETED;
        }
        return instanceSyncState;
    }

    private boolean isActualStatusDesired(Status actualStatus, Status... desiredStatuses) {
        boolean result = false;
        for (Status status : desiredStatuses) {
            if (actualStatus.equals(status)) {
                result = true;
            }
        }
        return result;
    }
}

