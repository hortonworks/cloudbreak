package com.sequenceiq.periscope.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.repository.ScalingPolicyRepository;

@Service
public class ScalingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingService.class);

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private ScalingPolicyRepository policyRepository;

    public ScalingPolicy createPolicy(Long clusterId, Long alertId, ScalingPolicy policy) {
        BaseAlert alert = alertService.getBaseAlert(clusterId, alertId);
        policy.setAlert(alert);
        ScalingPolicy scalingPolicy = policyRepository.save(policy);
        alert.setScalingPolicy(scalingPolicy);
        alertService.save(alert);
        LOGGER.info("Scaling policy [name: {}] was created for cluster [id: {}] attached to the alert [id: {}, name: {}]: {}",
                scalingPolicy.getName(), clusterId, alertId, alert.getName(), scalingPolicy);
        return scalingPolicy;
    }

    public ScalingPolicy updatePolicy(Long clusterId, Long policyId, ScalingPolicy scalingPolicy) {
        LOGGER.info("Updating scaling policy [id: {}] operation has been triggered for cluster [id: {}]", policyId, clusterId);
        ScalingPolicy policy = getScalingPolicy(clusterId, policyId);
        policy.setName(scalingPolicy.getName());
        policy.setHostGroup(scalingPolicy.getHostGroup());
        policy.setAdjustmentType(scalingPolicy.getAdjustmentType());
        policy.setScalingAdjustment(scalingPolicy.getScalingAdjustment());
        policy = policyRepository.save(policy);
        LOGGER.info("Scaling policy [name: {}] was updated for cluster [id: {}] attached to the alert [id: {}, name: {}]: {}",
                scalingPolicy.getName(), clusterId, policy.getAlertId(), policy.getAlert().getName(), scalingPolicy);
        return policy;
    }

    public void deletePolicy(Long clusterId, Long policyId) {
        ScalingPolicy policy = getScalingPolicy(clusterId, policyId);
        BaseAlert alert = policy.getAlert();
        alert.setScalingPolicy(null);
        policy.setAlert(null);
        policyRepository.delete(policy);
        alertService.save(alert);
        LOGGER.info("Scaling policy [name: {}] was deleted for cluster [id: {}] and detached from the alert [id: {}, name: {}]: {}",
                policy.getName(), clusterId, alert.getId(), alert.getName(), policy);
    }

    public List<ScalingPolicy> getPolicies(Long clusterId) {
        clusterService.findById(clusterId);
        return policyRepository.findAllByCluster(clusterId);
    }

    public ScalingPolicy save(ScalingPolicy policy) {
        return policyRepository.save(policy);
    }

    private ScalingPolicy getScalingPolicy(Long clusterId, Long policyId) {
        ScalingPolicy policy = policyRepository.findByCluster(clusterId, policyId);
        if (policy == null) {
            throw new NotFoundException("Scaling policy not found");
        }
        alertService.getBaseAlert(clusterId, policy.getAlertId());
        return policy;
    }

}
