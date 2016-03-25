package com.sequenceiq.periscope.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.repository.ScalingPolicyRepository;

@Service
public class ScalingService {

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private AlertService alertService;
    @Autowired
    private ScalingPolicyRepository policyRepository;

    public ScalingPolicy createPolicy(long clusterId, long alertId, ScalingPolicy policy) {
        BaseAlert alert = alertService.getBaseAlert(clusterId, alertId);
        policy.setAlert(alert);
        ScalingPolicy scalingPolicy = policyRepository.save(policy);
        alert.setScalingPolicy(scalingPolicy);
        alertService.save(alert);
        return scalingPolicy;
    }

    public ScalingPolicy updatePolicy(long clusterId, long policyId, ScalingPolicy scalingPolicy) {
        ScalingPolicy policy = getScalingPolicy(clusterId, policyId);
        policy.setName(scalingPolicy.getName());
        policy.setHostGroup(scalingPolicy.getHostGroup());
        policy.setAdjustmentType(scalingPolicy.getAdjustmentType());
        policy.setScalingAdjustment(scalingPolicy.getScalingAdjustment());
        return policyRepository.save(policy);
    }

    public void deletePolicy(long clusterId, long policyId) {
        ScalingPolicy policy = getScalingPolicy(clusterId, policyId);
        BaseAlert alert = policy.getAlert();
        alert.setScalingPolicy(null);
        policy.setAlert(null);
        policyRepository.delete(policy);
        alertService.save(alert);
    }

    public List<ScalingPolicy> getPolicies(long clusterId) {
        clusterService.findOneByUser(clusterId);
        return policyRepository.findAllByCluster(clusterId);
    }

    private ScalingPolicy getScalingPolicy(long clusterId, long policyId) {
        ScalingPolicy policy = policyRepository.findByCluster(clusterId, policyId);
        if (policy == null) {
            throw new NotFoundException("Scaling policy not found");
        }
        alertService.getBaseAlert(clusterId, policy.getAlertId());
        return policy;
    }

}
