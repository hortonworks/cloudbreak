package com.sequenceiq.periscope.service;

import static com.sequenceiq.periscope.utils.ClusterUtils.getTotalNodes;
import static java.lang.Math.abs;
import static java.util.Collections.singletonMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingPolicy;

@Component("ScalingRequest")
@Scope("prototype")
public class ScalingRequest implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingRequest.class);
    private static final int TIMEOUT = 1000 * 60 * 30;
    private static final int SLEEP = 5000;
    private final int desiredNodeCount;
    private final int totalNodes;
    private final Cluster cluster;
    private final ScalingPolicy policy;

    @Value("${cloudbreak.host}")
    private String host;
    @Value("${cloudbreak.port}")
    private int port;
    @Value("${cloudbreak.user}")
    private String user;
    @Value("${cloudbreak.pass}")
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
        long startTime = System.currentTimeMillis();
        try {
            LOGGER.info("Sending request to add {} instance(s) to cluster: {}", scalingAdjustment, clusterId);
            client.putStack(ambari, scalingAdjustment);
            boolean started = waitForInstancesToStart(cluster, startTime);
            if (started) {
                LOGGER.info("Sending request to add host group node '{}' to cluster: {}", hostGroup, clusterId);
                client.putCluster(ambari, singletonMap(hostGroup, scalingAdjustment));
            } else {
                LOGGER.info("Instance(s) didn't start in time, skipping scaling on cluster: {}", clusterId);
                // TODO should we terminate the launched instances?
            }
        } catch (Exception e) {
            LOGGER.error("Error adding nodes to cluster: " + clusterId, e);
        }
    }

    private void scaleDown(CloudbreakClient client, int scalingAdjustment) {
        String hostGroup = policy.getHostGroup();
        String ambari = cluster.getHost();
        long clusterId = cluster.getId();
        long startTime = System.currentTimeMillis();
        try {
            LOGGER.info("Sending request to remove host group node '{}' from cluster: {}", hostGroup, clusterId);
            client.putCluster(ambari, singletonMap(hostGroup, scalingAdjustment));
            boolean stopped = waitForInstancesToStop(cluster, scalingAdjustment, startTime);
            if (stopped) {
                LOGGER.info("Sending request to terminate {} instance(s) from cluster: {}", scalingAdjustment, clusterId);
                client.putStack(ambari, scalingAdjustment);
            } else {
                LOGGER.info("Instance(s) didn't stop in time, skipping scaling on cluster: {}", clusterId);
                // TODO should we force instance termination?
            }
        } catch (Exception e) {
            LOGGER.error("Error removing nodes from cluster: " + clusterId, e);
        }
    }

    private boolean waitForInstancesToStart(Cluster cluster, long requestStartTime) throws InterruptedException {
        boolean result = true;
        AmbariClient ambariClient = cluster.newAmbariClient();
        int currentNodes = totalNodes;
        while (currentNodes != desiredNodeCount && result) {
            if (isTimeExceeded(requestStartTime)) {
                result = false;
            }
            currentNodes = getTotalNodes(ambariClient);
            Thread.sleep(SLEEP);
            LOGGER.info("Waiting for instances to start on cluster: {}", cluster.getId());
        }
        return result;
    }

    private boolean waitForInstancesToStop(Cluster cluster, int scalingAdjustment, long requestStartTime)
            throws InterruptedException {
        boolean result = true;
        AmbariClient ambariClient = cluster.newAmbariClient();
        int removedNodes = 0;
        while (removedNodes != abs(scalingAdjustment) && result) {
            if (isTimeExceeded(requestStartTime)) {
                result = false;
            }
            removedNodes = ambariClient.getUnregisteredHostNames().size();
            Thread.sleep(SLEEP);
            LOGGER.info("Waiting for instances to stop on cluster: {}", cluster.getId());
        }
        return result;
    }

    private boolean isTimeExceeded(long startTime) {
        return (System.currentTimeMillis() - startTime) > TIMEOUT;
    }

}
