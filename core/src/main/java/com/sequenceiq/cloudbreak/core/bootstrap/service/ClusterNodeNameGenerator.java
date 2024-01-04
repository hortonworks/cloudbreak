package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
public class ClusterNodeNameGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterNodeNameGenerator.class);

    @Inject
    private HostDiscoveryService hostDiscoveryService;

    public String getNodeNameForInstanceMetadata(InstanceMetadataView im, StackView stack, Map<String, AtomicLong> hostGroupNodeIndexes,
            Set<String> clusterNodeNames) {
        if (isNotBlank(im.getShortHostname())) {
            LOGGER.info("Short hostname is filled for {}: {}", im.getInstanceId(), im.getShortHostname());
            return im.getShortHostname();
        } else {
            String generatedHostName;
            AtomicLong hostGroupNodeIndex = hostGroupNodeIndexes.computeIfAbsent(im.getInstanceGroupName(),
                        instantGroup -> new AtomicLong(0L));
            do {
                generatedHostName = hostDiscoveryService.calculateHostname(stack.getCustomHostname(), im.getShortHostname(),
                        im.getInstanceGroupName(), hostGroupNodeIndex.getAndIncrement(), stack.isHostgroupNameAsHostname());
            } while (clusterNodeNames.contains(generatedHostName));

            LOGGER.info("Generated hostname {} for address: {}", generatedHostName, im.getPrivateIp());
            clusterNodeNames.add(generatedHostName);
            return generatedHostName;
        }
    }
}
