package com.sequenceiq.cloudbreak.cloud.openstack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.heat.Stack;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.options.PortListOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

@Component
public class OpenStackMetadataCollector implements MetadataCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackMetadataCollector.class);

    @Inject
    private OpenStackClient openStackClient;

    @Inject
    private OpenStackHeatUtils utils;

    public List<CloudVmInstanceStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<InstanceTemplate> vms) {
        CloudResource resource = utils.getHeatResource(resources);

        String stackName = authenticatedContext.getCloudContext().getStackName();
        String heatStackId = resource.getName();

        Map<String, InstanceTemplate> templateMap = Maps.uniqueIndex(vms, new Function<InstanceTemplate, String>() {
            public String apply(InstanceTemplate from) {
                return utils.getPrivateInstanceId(from.getGroupName(), Long.toString(from.getPrivateId()));
            }
        });

        OSClient client = openStackClient.createOSClient(authenticatedContext);

        Stack heatStack = client.heat().stacks().getDetails(stackName, heatStackId);

        List<CloudVmInstanceStatus> results = new ArrayList<>();


        List<Map<String, Object>> outputs = heatStack.getOutputs();
        for (Map<String, Object> map : outputs) {
            String instanceUUID = (String) map.get("output_value");
            Server server = client.compute().servers().get(instanceUUID);
            Map<String, String> metadata = server.getMetadata();
            String privateInstanceId = utils.getPrivateInstanceId(metadata);
            InstanceTemplate template = templateMap.get(privateInstanceId);
            if (template != null) {
                CloudInstance cloudInstance = createInstanceMetaData(client, server, instanceUUID, template);
                results.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED));
            }
        }

        return results;
    }

    private CloudInstance createInstanceMetaData(OSClient client, Server server, String instanceId, InstanceTemplate template) {
        CloudInstanceMetaData md = CloudInstanceMetaDataExtractor.getMetadataExtractor(client, server, instanceId).extract();
        return new CloudInstance(instanceId, md, template);
    }

    private abstract static class CloudInstanceMetaDataExtractor {

        private final OSClient client;
        private final Server server;
        private final String instanceId;

        private CloudInstanceMetaDataExtractor(OSClient client, Server server, String instanceId) {
            this.client = client;
            this.server = server;
            this.instanceId = instanceId;
        }

        private static CloudInstanceMetaDataExtractor getMetadataExtractor(OSClient client, Server server, String instanceId) {
            if (server.getAddresses().getAddresses().isEmpty()) {
                return new PortApiExtractor(client, server, instanceId);
            } else {
                return new ComputeApiExtractor(client, server, instanceId);
            }
        }

        public abstract CloudInstanceMetaData extract();

        public OSClient getClient() {
            return client;
        }

        public Server getServer() {
            return server;
        }

        public String getInstanceId() {
            return instanceId;
        }

        private static class PortApiExtractor extends CloudInstanceMetaDataExtractor {

            public PortApiExtractor(OSClient client, Server server, String instanceId) {
                super(client, server, instanceId);
            }

            @Override
            public CloudInstanceMetaData extract() {
                LOGGER.debug("Address map was empty, trying to extract ips");
                List<? extends Port> ports = getClient().networking().port().list(getPortListOptions());
                String portId = ports.get(0).getId();
                List<? extends NetFloatingIP> floatingIps = getClient().networking().floatingip().list(getFloatingIpListOptions(portId));
                NetFloatingIP ips = floatingIps.get(0);
                LOGGER.info("PrivateIp of instance: {} is {}", getServer().getName(), ips.getFixedIpAddress());
                LOGGER.info("FloatingIp of instance: {} is {}", getServer().getName(), ips.getFloatingIpAddress());
                return new CloudInstanceMetaData(ips.getFixedIpAddress(), ips.getFloatingIpAddress());
            }

            private PortListOptions getPortListOptions() {
                return PortListOptions.create().deviceId(getInstanceId());
            }

            private Map<String, String> getFloatingIpListOptions(String portId) {
                Map<String, String> paramMap = Maps.newHashMap();
                paramMap.put("port_id", portId);
                return paramMap;
            }
        }

        private static class ComputeApiExtractor extends CloudInstanceMetaDataExtractor {

            public ComputeApiExtractor(OSClient client, Server server, String instanceId) {
                super(client, server, instanceId);
            }

            @Override
            public CloudInstanceMetaData extract() {
                String privateIp = null;
                String floatingIp = null;
                Map<String, List<? extends Address>> adrMap = getServer().getAddresses().getAddresses();
                LOGGER.debug("Address map: {} of instance: {}", adrMap, getServer().getName());
                for (String key : adrMap.keySet()) {
                    LOGGER.debug("Network resource key: {} of instance: {}", key, getServer().getName());
                    List<? extends Address> adrList = adrMap.get(key);
                    for (Address adr : adrList) {
                        LOGGER.debug("Network resource key: {} of instance: {}, address: {}", key, getServer().getName(), adr);
                        switch (adr.getType()) {
                            case "fixed":
                                privateIp = adr.getAddr();
                                LOGGER.info("PrivateIp of instance: {} is {}", getServer().getName(), privateIp);
                                break;
                            case "floating":
                                floatingIp = adr.getAddr();
                                LOGGER.info("FloatingIp of instance: {} is {}", getServer().getName(), floatingIp);
                                break;
                            default:
                                LOGGER.error("No such network resource type: {}, instance: {}", adr.getType(), getServer().getName());
                        }
                    }
                }
                return new CloudInstanceMetaData(privateIp, floatingIp);
            }
        }
    }
}
