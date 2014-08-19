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
        if (desiredNodeCount > totalNodes) {
            scaleUpTo(cluster, desiredNodeCount);
        } else if (desiredNodeCount < totalNodes) {
            scaleDownTo(cluster, desiredNodeCount);
        } else {
            LOGGER.info("No scaling activity required on {}", cluster.getId());
        }
    }

    public void scaleUpTo(Cluster cluster, int nodeCount) {
        LOGGER.info("Should add {} new nodes with cloudbreak to {}", nodeCount - cluster.getTotalNodes(), cluster.getId());
    }

    public void scaleDownTo(Cluster cluster, int nodeCount) {
        LOGGER.info("Should remove {} nodes with cloudbreak from {}", cluster.getTotalNodes() - nodeCount, cluster.getId());
    }

    public ScalingPolicies setScalingPolicies(String clusterId, ScalingPolicies scalingPolicies) throws ClusterNotFoundException {
        ClusterDetails clusterDetails = clusterDetailsRepository.findOne(clusterId);
        clusterDetails.setCoolDown(scalingPolicies.getCoolDown());
        clusterDetails.setMinSize(scalingPolicies.getMinSize());
        clusterDetails.setMaxSize(scalingPolicies.getMaxSize());
        clusterService.get(clusterId).setClusterDetails(clusterDetails);
        clusterDetailsRepository.save(clusterDetails);
        return getScalingPolicies(clusterDetails);
    }

    public ScalingPolicies getScalingPolicies(String clusterId) throws ClusterNotFoundException {
        return getScalingPolicies(clusterService.get(clusterId).getClusterDetails());
    }

    public ScalingPolicies getScalingPolicies(ClusterDetails clusterDetails) throws ClusterNotFoundException {
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

    private int getDesiredNodeCount(Cluster cluster, ScalingPolicy policy) {
        int scalingAdjustment = policy.getScalingAdjustment();
        switch (policy.getAdjustmentType()) {
            case NODE_COUNT:
                return cluster.getTotalNodes() + scalingAdjustment;
            case PERCENTAGE:
                return cluster.getTotalNodes() + (int) (ceil(cluster.getTotalNodes() * (Double.valueOf(scalingAdjustment) / ClusterUtils.MAX_CAPACITY)));
            default:
                return cluster.getTotalNodes();
        }
    }
}
