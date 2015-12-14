package com.sequenceiq.cloudbreak.cloud.openstack.metadata;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;

@Component
public class ComputeApiExtractor implements CloudInstanceMetaDataExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeApiExtractor.class);

    @Inject
    private HypervisorExtractor hypervisorExtractor;

    @Override
    public CloudInstanceMetaData extractMetadata(OSClient client, Server server, String instanceId) {
        String hypervisor = hypervisorExtractor.getHypervisor(server);
        String privateIp = null;
        String floatingIp = null;
        Map<String, List<? extends Address>> adrMap = server.getAddresses().getAddresses();
        LOGGER.debug("Address map: {} of instance: {}", adrMap, server.getName());
        for (String key : adrMap.keySet()) {
            LOGGER.debug("Network resource key: {} of instance: {}", key, server.getName());
            List<? extends Address> adrList = adrMap.get(key);
            for (Address adr : adrList) {
                LOGGER.debug("Network resource key: {} of instance: {}, address: {}", key, server.getName(), adr);
                switch (adr.getType()) {
                    case "fixed":
                        privateIp = adr.getAddr();
                        LOGGER.info("PrivateIp of instance: {} is {}", server.getName(), privateIp);
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
        return new CloudInstanceMetaData(privateIp, floatingIp, hypervisor);
    }
}
