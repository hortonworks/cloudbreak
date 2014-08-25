package com.sequenceiq.periscope.service;

import static java.lang.Math.ceil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.Alarm;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.model.ScalingPolicies;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.utils.ClusterUtils;

@Service
public class ScalingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingService.class);

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ClusterRepository clusterRepository;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private ApplicationContext applicationContext;

    public void scale(Cluster cluster, ScalingPolicy policy) {
        if (canScale(cluster)) {
            int totalNodes = ClusterUtils.getTotalNodes(cluster);
            int desiredNodeCount = getDesiredNodeCount(cluster, policy, totalNodes);
            if (totalNodes != desiredNodeCount) {
                LOGGER.info("Sending scaling request for cluster: {}", cluster.getId());
                ScalingRequest scalingRequest = (ScalingRequest)
                        applicationContext.getBean("ScalingRequest", cluster, policy, totalNodes, desiredNodeCount);
                executorService.execute(scalingRequest);
                Alarm alarm = policy.getAlarm();
                alarm.reset();
                cluster.setLastScalingActivityCurrent();
                LOGGER.info("Resetting time on alarm: {} on cluster: {}", alarm.getName(), cluster.getId());
            } else {
                LOGGER.info("No scaling activity required on {}", cluster.getId());
            }
        } else {
            LOGGER.info("Cluster: {} is in cooling state, cannot scale", cluster.getId());
        }
    }

    public ScalingPolicies setScalingPolicies(long clusterId, ScalingPolicies scalingPolicies) throws ClusterNotFoundException {
        Cluster cluster = clusterService.get(clusterId);
        cluster.setCoolDown(scalingPolicies.getCoolDown());
        cluster.setMinSize(scalingPolicies.getMinSize());
        cluster.setMaxSize(scalingPolicies.getMaxSize());
        List<ScalingPolicy> policies = scalingPolicies.getScalingPolicies();
        List<Alarm> alarms = clusterRepository.findOne(clusterId).getAlarms();
        for (Alarm alarm : alarms) {
            if (!policies.contains(alarm.getScalingPolicy())) {
                alarm.setScalingPolicy(null);
            }
        }
        cluster.setAlarms(alarms);
        clusterRepository.save(cluster);
        return getScalingPolicies(cluster);
    }

    public ScalingPolicies addScalingPolicy(long clusterId) throws ClusterNotFoundException, NoScalingGroupException {
        Cluster cluster = clusterService.get(clusterId);
        if (cluster.getCoolDown() == -1 || cluster.getMinSize() == -1 || cluster.getMaxSize() == -1) {
            throw new NoScalingGroupException(clusterId,
                    "Scaling parameters are not provided (cooldown, minSize, maxSize). Use POST first.");
        }
        cluster.setAlarms(clusterRepository.findOne(clusterId).getAlarms());
        clusterRepository.save(cluster);
        return getScalingPolicies(cluster);
    }

    public ScalingPolicies deletePolicy(long clusterId, long policyId) throws ClusterNotFoundException {
        Cluster runningCluster = clusterService.get(clusterId);
        Cluster savedCluster = clusterRepository.findOne(clusterId);
        List<Alarm> alarms = savedCluster.getAlarms();
        for (Alarm alarm : alarms) {
            ScalingPolicy scalingPolicy = alarm.getScalingPolicy();
            if (scalingPolicy != null && scalingPolicy.getId() == policyId) {
                alarm.setScalingPolicy(null);
                break;
            }
        }
        clusterRepository.save(savedCluster);
        runningCluster.setAlarms(alarms);
        return getScalingPolicies(clusterId);
    }

    public ScalingPolicies getScalingPolicies(long clusterId) throws ClusterNotFoundException {
        return getScalingPolicies(clusterService.get(clusterId));
    }

    public ScalingPolicies getScalingPolicies(Cluster cluster) {
        ScalingPolicies group = new ScalingPolicies();
        group.setMaxSize(cluster.getMaxSize());
        group.setMinSize(cluster.getMinSize());
        group.setCoolDown(cluster.getCoolDown());
        List<ScalingPolicy> policies = new ArrayList<>();
        for (Alarm alarm : cluster.getAlarms()) {
            ScalingPolicy scalingPolicy = alarm.getScalingPolicy();
            if (scalingPolicy != null) {
                policies.add(scalingPolicy);
            }
        }
        group.setScalingPolicies(policies);
        return group;
    }

    private boolean canScale(Cluster cluster) {
        int coolDown = cluster.getCoolDown();
        long lastScalingActivity = cluster.getLastScalingActivity();
        return lastScalingActivity == 0
                || (System.currentTimeMillis() - lastScalingActivity) > (coolDown * ClusterUtils.MIN_IN_MS);
    }

    private int getDesiredNodeCount(Cluster cluster, ScalingPolicy policy, int totalNodes) {
        int scalingAdjustment = policy.getScalingAdjustment();
        int desiredNodeCount;
        switch (policy.getAdjustmentType()) {
            case NODE_COUNT:
                desiredNodeCount = totalNodes + scalingAdjustment;
                break;
            case PERCENTAGE:
                desiredNodeCount = totalNodes
                        + (int) (ceil(totalNodes * ((double) scalingAdjustment / ClusterUtils.MAX_CAPACITY)));
                break;
            default:
                desiredNodeCount = totalNodes;
        }
        int minSize = cluster.getMinSize();
        int maxSize = cluster.getMaxSize();
        return desiredNodeCount < minSize ? minSize : desiredNodeCount > maxSize ? maxSize : desiredNodeCount;
    }
}
