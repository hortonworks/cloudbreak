package com.sequenceiq.periscope.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.model.Alarm;
import com.sequenceiq.periscope.model.AutoScalingGroup;
import com.sequenceiq.periscope.model.ScalingPolicy;
import com.sequenceiq.periscope.registry.Cluster;
import com.sequenceiq.periscope.utils.ClusterUtils;

@Service
public class ScalingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingService.class);

    @Autowired
    private ClusterService clusterService;

    public void scale(Cluster cluster, ScalingPolicy policy) {
        int desiredNodeCount = getDesiredNodeCount(cluster, policy);
        int totalNodes = cluster.getTotalNodes();
        if (desiredNodeCount > totalNodes) {
            scaleUpTo(cluster, desiredNodeCount);
        } else if (desiredNodeCount < totalNodes) {
            scaleDownTo(cluster, desiredNodeCount);
        } else {
            LOGGER.info("No scaling activity on {}", cluster.getId());
        }
    }

    public void scaleUpTo(Cluster cluster, int nodeCount) {
        LOGGER.info("Should add {} new nodes with cloudbreak to {}", nodeCount - cluster.getTotalNodes(), cluster.getId());
    }

    public void scaleDownTo(Cluster cluster, int nodeCount) {
        LOGGER.info("Should remove {} nodes with cloudbreak from {}", cluster.getTotalNodes() - nodeCount, cluster.getId());
    }

    public void setAlarms(String clusterId, List<Alarm> alarms) throws ClusterNotFoundException {
        Cluster cluster = clusterService.get(clusterId);
        cluster.setAlarms(alarms);
    }

    public List<Alarm> getAlarms(String clusterId) throws ClusterNotFoundException {
        return clusterService.get(clusterId).getAlarms();
    }

    public ScalingPolicy getScalingPolicy(Cluster cluster, String policyId) {
        ScalingPolicy policy = null;
        AutoScalingGroup autoScalingGroup = cluster.getAutoScalingGroup();
        if (autoScalingGroup != null) {
            for (ScalingPolicy scalingPolicy : autoScalingGroup.getScalingPolicies()) {
                if (scalingPolicy.getId().equals(policyId)) {
                    policy = scalingPolicy;
                    break;
                }
            }
        }
        return policy;
    }

    public Alarm getAlarm(Cluster cluster, String alarmId) {
        Alarm result = null;
        for (Alarm alarm : cluster.getAlarms()) {
            if (alarm.getId().equals(alarmId)) {
                result = alarm;
                break;
            }
        }
        return result;
    }

    public AutoScalingGroup getAutoScalingGroup(String clusterId) throws ClusterNotFoundException {
        return clusterService.get(clusterId).getAutoScalingGroup();
    }

    public void setAutoScalingGroup(String clusterId, AutoScalingGroup autoScalingGroup) throws ClusterNotFoundException {
        clusterService.get(clusterId).setAutoScalingGroup(autoScalingGroup);
    }

    private int getDesiredNodeCount(Cluster cluster, ScalingPolicy policy) {
        int scalingAdjustment = policy.getScalingAdjustment();
        switch (policy.getAdjustmentType()) {
            case NODE_COUNT:
                return cluster.getTotalNodes() + scalingAdjustment;
            case PERCENTAGE:
                return cluster.getTotalNodes() + cluster.getTotalNodes() * (scalingAdjustment / ClusterUtils.MAX_CAPACITY);
            default:
                return cluster.getTotalNodes();
        }
    }
}
