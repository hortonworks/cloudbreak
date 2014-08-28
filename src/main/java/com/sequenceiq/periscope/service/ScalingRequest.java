package com.sequenceiq.periscope.service;

import static java.util.Collections.singletonMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;

@Component("ScalingRequest")
@Scope("prototype")
public class ScalingRequest implements Runnable {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(ScalingRequest.class);
    private static final String AVAILABLE = "AVAILABLE";
    private static final int RETRY_COUNT = 100;
    private static final int SLEEP = 10000;
    private final int desiredNodeCount;
    private final int totalNodes;
    private final Cluster cluster;
    private final ScalingPolicy policy;

    @Value("${periscope.cloudbreak.host}")
    private String host;
    @Value("${periscope.cloudbreak.port}")
    private int port;
    @Value("${periscope.cloudbreak.user}")
    private String user;
    @Value("${periscope.cloudbreak.pass}")
    private String pass;

    public ScalingRequest(Cluster cluster, ScalingPolicy policy, int totalNodes, int desiredNodeCount) {
        this.cluster = cluster;
        this.policy = policy;
        this.totalNodes = totalNodes;
        this.desiredNodeCount = desiredNodeCount;
    }

    @Override
    public void run() {
        CloudbreakClient client = new CloudbreakClient(host, port, user, pass);
        int scalingAdjustment = desiredNodeCount - totalNodes;
        if (scalingAdjustment > 0) {
            scaleUp(client, scalingAdjustment);
        } else {
            scaleDown(client, scalingAdjustment);
        }
    }

    private void scaleUp(CloudbreakClient client, int scalingAdjustment) {
        String hostGroup = policy.getHostGroup();
        String ambari = cluster.getHost();
        long clusterId = cluster.getId();
        try {
            LOGGER.info(clusterId, "Sending request to add {} instance(s)", scalingAdjustment);
            int stackId = client.putStack(ambari, scalingAdjustment);
            boolean ready = waitForReadyState(clusterId, stackId, client);
            if (ready) {
                LOGGER.info(clusterId, "Sending request to install components to the host(s)");
                client.putCluster(ambari, singletonMap(hostGroup, scalingAdjustment));
            } else {
                LOGGER.info(clusterId, "Instance(s) didn't start in time, skipping scaling");
                // TODO should we terminate the launched instances?
            }
        } catch (Exception e) {
            LOGGER.error(clusterId, "Error adding nodes to cluster", e);
        }
    }

    private void scaleDown(CloudbreakClient client, int scalingAdjustment) {
        String hostGroup = policy.getHostGroup();
        String ambari = cluster.getHost();
        long clusterId = cluster.getId();
        try {
            LOGGER.info(clusterId, "Sending request to remove node(s) from host group '{}'", hostGroup);
            int stackId = client.putCluster(ambari, singletonMap(hostGroup, scalingAdjustment));
            boolean ready = waitForReadyState(clusterId, stackId, client);
            if (ready) {
                LOGGER.info(clusterId, "Sending request to terminate instance(s)");
                client.putStack(ambari, scalingAdjustment);
            } else {
                LOGGER.info(clusterId, "Instance(s) didn't stop in time, skipping scaling");
                // TODO should we force instance termination?
            }
        } catch (Exception e) {
            LOGGER.error(clusterId, "Error removing nodes from the cluster", e);
        }
    }

    private boolean waitForReadyState(long clusterId, int stackId, CloudbreakClient client) throws InterruptedException {
        boolean result = false;
        int retry = 0;
        while (retry < RETRY_COUNT && !result) {
            LOGGER.info(clusterId, "Scaling: Waiting for cluster to be {}", AVAILABLE);
            String status = client.getStackStatus(stackId);
            if (AVAILABLE.equals(status)) {
                result = true;
            } else {
                retry++;
            }
            Thread.sleep(SLEEP);
        }
        return result;
    }

}
