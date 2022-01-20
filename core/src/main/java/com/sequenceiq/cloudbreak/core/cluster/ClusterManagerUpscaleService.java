package com.sequenceiq.cloudbreak.core.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class ClusterManagerUpscaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerUpscaleService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterHostServiceRunner hostRunner;

    @Inject
    private ClusterServiceRunner clusterServiceRunner;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    public void upscaleClusterManager(Long stackId, String hostGroupName, Integer scalingAdjustment, boolean primaryGatewayChanged)
            throws ClusterClientInitException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.debug("Adding {} new nodes for host group {}", scalingAdjustment, hostGroupName);
        Map<String, List<String>> hostsPerHostGroup = new HashMap<>();

        Map<String, String> hosts = hostRunner.addClusterServices(stackId, hostGroupName, scalingAdjustment);
        if (primaryGatewayChanged) {
            clusterServiceRunner.updateAmbariClientConfig(stack, stack.getCluster());
        }
        for (String hostName : hosts.keySet()) {
            if (!hostsPerHostGroup.containsKey(hostGroupName)) {
                hostsPerHostGroup.put(hostGroupName, new ArrayList<>());
            }
            hostsPerHostGroup.get(hostGroupName).add(hostName);
        }
        clusterService.updateInstancesToRunning(stack.getCluster().getId(), hostsPerHostGroup);

        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        connector.waitForHosts(stackService.getByIdWithListsInTransaction(stackId).getRunningInstanceMetaDataSet());
    }
}
