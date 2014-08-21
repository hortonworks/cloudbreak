package com.sequenceiq.periscope.service;

import static java.lang.Math.ceil;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.model.Alarm;
import com.sequenceiq.periscope.model.Cluster;
import com.sequenceiq.periscope.model.ClusterDetails;
import com.sequenceiq.periscope.model.ScalingPolicies;
import com.sequenceiq.periscope.model.ScalingPolicy;
import com.sequenceiq.periscope.repository.AlarmRepository;
import com.sequenceiq.periscope.repository.ClusterDetailsRepository;
import com.sequenceiq.periscope.utils.ClusterUtils;

@Service
public class ScalingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingService.class);

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ClusterDetailsRepository clusterDetailsRepository;
    @Autowired
    private AlarmRepository alarmRepository;

    public void scale(Cluster cluster, ScalingPolicy policy) {
        int desiredNodeCount = getDesiredNodeCount(cluster, policy);
        int totalNodes = cluster.getTotalNodes();
        if (totalNodes != desiredNodeCount) {
            scale(cluster, policy, totalNodes, desiredNodeCount);
        } else {
            LOGGER.info("No scaling activity required on {}", cluster.getId());
        }
    }

    public ScalingPolicies setScalingPolicies(long clusterId, ScalingPolicies scalingPolicies) throws ClusterNotFoundException {
        ClusterDetails clusterDetails = clusterDetailsRepository.findOne(clusterId);
        clusterDetails.setCoolDown(scalingPolicies.getCoolDown());
        clusterDetails.setMinSize(scalingPolicies.getMinSize());
        clusterDetails.setMaxSize(scalingPolicies.getMaxSize());
        clusterService.get(clusterId).setClusterDetails(clusterDetails);
        clusterDetailsRepository.save(clusterDetails);
        return getScalingPolicies(clusterDetails);
    }

    public ScalingPolicies deletePolicy(long clusterId, long policyId) throws ClusterNotFoundException {
        ClusterDetails clusterDetails = clusterDetailsRepository.findOne(clusterId);
        for (Alarm alarm : clusterDetails.getAlarms()) {
            ScalingPolicy scalingPolicy = alarm.getScalingPolicy();
            if (scalingPolicy != null && scalingPolicy.getId() == policyId) {
                alarm.setScalingPolicy(null);
                break;
            }
        }
        clusterService.get(clusterId).setClusterDetails(clusterDetails);
        clusterDetailsRepository.save(clusterDetails);
        return getScalingPolicies(clusterId);
    }

    public ScalingPolicies getScalingPolicies(long clusterId) throws ClusterNotFoundException {
        return getScalingPolicies(clusterService.get(clusterId).getClusterDetails());
    }

    public ScalingPolicies getScalingPolicies(ClusterDetails clusterDetails) {
        ScalingPolicies group = new ScalingPolicies();
        group.setMaxSize(clusterDetails.getMaxSize());
        group.setMinSize(clusterDetails.getMinSize());
        group.setCoolDown(clusterDetails.getCoolDown());
        List<ScalingPolicy> policies = new ArrayList<>();
        for (Alarm alarm : clusterDetails.getAlarms()) {
            ScalingPolicy scalingPolicy = alarm.getScalingPolicy();
            if (scalingPolicy != null) {
                policies.add(scalingPolicy);
            }
        }
        group.setScalingPolicies(policies);
        return group;
    }

    private void scale(Cluster cluster, ScalingPolicy policy, int totalNodes, int desiredNodeCount) {
        if (canScale(cluster)) {
            if (desiredNodeCount > totalNodes) {
                scaleUpTo(cluster, desiredNodeCount);
            } else if (desiredNodeCount < totalNodes) {
                scaleDownTo(cluster, desiredNodeCount);
            }
            Alarm alarm = policy.getAlarm();
            alarm.resetAlarmHitsSince();
            cluster.setLastScalingActivityCurrent();
            LOGGER.info("Resetting time on alarm: {} on cluster: {}", alarm.getAlarmName(), cluster.getId());
        } else {
            LOGGER.info("Cluster: {} is in cooling state, cannot scale", cluster.getId());
        }
    }

    private boolean canScale(Cluster cluster) {
        int coolDown = cluster.getCoolDown();
        long lastScalingActivity = cluster.getLastScalingActivity();
        return lastScalingActivity == 0
                || (System.currentTimeMillis() - lastScalingActivity) > (coolDown * ClusterUtils.MIN_IN_MS);
    }

    private void scaleUpTo(Cluster cluster, int nodeCount) {
        LOGGER.info("Should add {} new nodes with cloudbreak to {}", nodeCount - cluster.getTotalNodes(), cluster.getId());
    }

    private void scaleDownTo(Cluster cluster, int nodeCount) {
        LOGGER.info("Should remove {} nodes with cloudbreak from {}", cluster.getTotalNodes() - nodeCount, cluster.getId());
    }

    private int getDesiredNodeCount(Cluster cluster, ScalingPolicy policy) {
        int scalingAdjustment = policy.getScalingAdjustment();
        int desiredNodeCount;
        switch (policy.getAdjustmentType()) {
            case NODE_COUNT:
                desiredNodeCount = cluster.getTotalNodes() + scalingAdjustment;
                break;
            case PERCENTAGE:
                desiredNodeCount = cluster.getTotalNodes()
                        + (int) (ceil(cluster.getTotalNodes() * ((double) scalingAdjustment / ClusterUtils.MAX_CAPACITY)));
                break;
            default:
                desiredNodeCount = cluster.getTotalNodes();
        }
        int minSize = cluster.getMinSize();
        int maxSize = cluster.getMaxSize();
        return desiredNodeCount < minSize ? minSize : desiredNodeCount > maxSize ? maxSize : desiredNodeCount;
    }
}
