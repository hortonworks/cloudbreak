package com.sequenceiq.cloudbreak.cloud.openstack.metadata;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.options.PortListOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;

@Component
public class PortApiExtractor implements CloudInstanceMetaDataExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortApiExtractor.class);

    @Override
    public CloudInstanceMetaData extractMetadata(OSClient client, Server server, String instanceId) {
        LOGGER.debug("Address map was empty, trying to extract ips");
        List<? extends Port> ports = client.networking().port().list(getPortListOptions(instanceId));
        String portId = ports.get(0).getId();
        List<? extends NetFloatingIP> floatingIps = client.networking().floatingip().list(getFloatingIpListOptions(portId));
        NetFloatingIP ips = floatingIps.get(0);
        LOGGER.info("PrivateIp of instance: {} is {}", server.getName(), ips.getFixedIpAddress());
        LOGGER.info("FloatingIp of instance: {} is {}", server.getName(), ips.getFloatingIpAddress());
        return new CloudInstanceMetaData(ips.getFixedIpAddress(), ips.getFloatingIpAddress());
    }

    private PortListOptions getPortListOptions(String instanceId) {
        return PortListOptions.create().deviceId(instanceId);
    }

    private Map<String, String> getFloatingIpListOptions(String portId) {
        Map<String, String> paramMap = Maps.newHashMap();
        paramMap.put("port_id", portId);
        return paramMap;
    }
}
