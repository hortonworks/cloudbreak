package com.sequenceiq.periscope.service;

import static java.lang.Math.ceil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.BaseAlarm;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricAlarm;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlarm;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.MetricAlarmRepository;
import com.sequenceiq.periscope.repository.ScalingPolicyRepository;
import com.sequenceiq.periscope.repository.TimeAlarmRepository;
import com.sequenceiq.periscope.utils.ClusterUtils;

@Service
public class ScalingService {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(ScalingService.class);

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ClusterRepository clusterRepository;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private TimeAlarmRepository timeAlarmRepository;
    @Autowired
    private MetricAlarmRepository metricAlarmRepository;
    @Autowired
    private ScalingPolicyRepository policyRepository;

    public void scale(Cluster cluster, ScalingPolicy policy) {
        long clusterId = cluster.getId();
        if (canScale(cluster)) {
            int totalNodes = ClusterUtils.getTotalNodes(cluster);
            int desiredNodeCount = getDesiredNodeCount(cluster, policy, totalNodes);
            if (totalNodes != desiredNodeCount) {
                ScalingRequest scalingRequest = (ScalingRequest)
                        applicationContext.getBean("ScalingRequest", cluster, policy, totalNodes, desiredNodeCount);
                executorService.execute(scalingRequest);
                BaseAlarm alarm = policy.getAlarm();
                alarm.reset();
                cluster.setLastScalingActivityCurrent();
                LOGGER.info(clusterId, "Resetting time on alarm: {}", alarm.getName());
            } else {
                LOGGER.info(clusterId, "No scaling activity required");
            }
        } else {
            LOGGER.info(clusterId, "Cluster is in cooling state, cannot scale");
        }
    }

    public ScalingPolicy setScalingPolicy(PeriscopeUser user, long clusterId, long policyId, ScalingPolicy scalingPolicy)
            throws ClusterNotFoundException {
        Cluster cluster = clusterService.get(user, clusterId);
        ScalingPolicy result = null;
        List<BaseAlarm> alarms = cluster.getAlarms();
        for (BaseAlarm alarm : alarms) {
            ScalingPolicy policy = alarm.getScalingPolicy();
            if (policy != null && policy.getId() == policyId) {
                policy.setName(scalingPolicy.getName());
                policy.setHostGroup(scalingPolicy.getHostGroup());
                policy.setAdjustmentType(scalingPolicy.getAdjustmentType());
                policy.setScalingAdjustment(scalingPolicy.getScalingAdjustment());
                saveAlarm(alarm);
                result = policy;
            }
        }
        if (result == null) {
            throw new ScalingPolicyNotFoundException(policyId);
        }
        return result;
    }

    public ScalingPolicy addScalingPolicy(PeriscopeUser user, long clusterId, ScalingPolicy policy) throws ClusterNotFoundException {
        Cluster cluster = clusterService.get(user, clusterId);
        long alarmId = policy.getAlarm().getId();
        List<BaseAlarm> alarms = cluster.getAlarms();
        for (BaseAlarm baseAlarm : alarms) {
            if (baseAlarm.getId() == alarmId) {
                baseAlarm.setScalingPolicy(policy);
                policyRepository.save(policy);
                saveAlarm(baseAlarm);
                break;
            }
        }
        return policy;
    }

    public void deletePolicy(PeriscopeUser user, long clusterId, long policyId) throws ClusterNotFoundException {
        Cluster cluster = clusterService.get(user, clusterId);
        List<BaseAlarm> alarms = cluster.getAlarms();
        for (BaseAlarm alarm : alarms) {
            ScalingPolicy scalingPolicy = alarm.getScalingPolicy();
            if (scalingPolicy != null && scalingPolicy.getId() == policyId) {
                alarm.setScalingPolicy(null);
                saveAlarm(alarm);
                break;
            }
        }
        clusterRepository.save(cluster);
        cluster.setAlarms(alarms);
    }

    public List<ScalingPolicy> getScalingPolicies(PeriscopeUser user, long clusterId) throws ClusterNotFoundException {
        return getScalingPolicies(clusterService.get(user, clusterId));
    }

    public List<ScalingPolicy> getScalingPolicies(Cluster cluster) {
        List<ScalingPolicy> policies = new ArrayList<>();
        for (BaseAlarm alarm : cluster.getAlarms()) {
            ScalingPolicy scalingPolicy = alarm.getScalingPolicy();
            if (scalingPolicy != null) {
                policies.add(scalingPolicy);
            }
        }
        return policies;
    }

    private void saveAlarm(BaseAlarm alarm) {
        if (alarm instanceof TimeAlarm) {
            timeAlarmRepository.save((TimeAlarm) alarm);
        } else {
            metricAlarmRepository.save((MetricAlarm) alarm);
        }
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
            case EXACT:
                desiredNodeCount = policy.getScalingAdjustment();
                break;
            default:
                desiredNodeCount = totalNodes;
        }
        int minSize = cluster.getMinSize();
        int maxSize = cluster.getMaxSize();
        return desiredNodeCount < minSize ? minSize : desiredNodeCount > maxSize ? maxSize : desiredNodeCount;
    }
}
