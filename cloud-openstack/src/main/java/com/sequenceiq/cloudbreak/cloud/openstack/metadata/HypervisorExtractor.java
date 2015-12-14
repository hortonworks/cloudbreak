package com.sequenceiq.cloudbreak.cloud.openstack.metadata;

import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HypervisorExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HypervisorExtractor.class);

    public String getHypervisor(Server server) {
        LOGGER.info("Hypervisor info for instance: {}. HypervisorHostname: {}, Host: {}", server.getInstanceName(), server.getHypervisorHostname(), server
                .getHost());
        String hypervisor = server.getHypervisorHostname();
        LOGGER.info("Hypervisor for instance: {} is: {}", server.getInstanceName(), server.getHypervisorHostname(), server.getHost());
        if (hypervisor == null) {
            hypervisor = server.getHost();
        }
        LOGGER.info("Used hypervisor for instance: {}. hypervisor: {}", server.getInstanceName(), hypervisor);
        return hypervisor;
    }
}
