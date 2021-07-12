package com.sequenceiq.cloudbreak.cmtemplate.configproviders.cruisecontrol;


import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_11;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmHostGroupRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class CruiseControlRoleConfigProvider implements CmHostGroupRoleConfigProvider {

    private static final String DEFAULT_GOALS = "default.goals";

    private static final String HARD_GOALS = "hard.goals";

    private static final String ANOMALY_DETECTION_GOALS = "anomaly.detection.goals";

    private static final String SELF_HEALING_ENABLED = "self.healing.enabled";

    private static final String ANOMALY_NOTIFIER_CLASS = "anomaly.notifier.class";

    private static final String METRIC_ANOMALY_FINDER_CLASS = "metric.anomaly.finder.class";

    private static final String CPU_BALANCE_THRESHOLD = "cpu.balance.threshold";

    private static final String DISK_BALANCE_THRESHOLD = "disk.balance.threshold";

    private static final String NETWORK_INBOUND_BALANCE_THRESHOLD = "network.inbound.balance.threshold";

    private static final String NETWORK_OUTBOUND_BALANCE_THRESHOLD = "network.outbound.balance.threshold";

    private static final String REPLICA_COUNT_BALANCE_THRESHOLD = "replica.count.balance.threshold";

    private static final String CPU_CAPACITY_THRESHOLD = "cpu.capacity.threshold";

    private static final String DISK_CAPACITY_THRESHOLD = "disk.capacity.threshold";

    private static final String NETWORK_INBOUND_CAPACITY_THRESHOLD = "network.inbound.capacity.threshold";

    private static final String NETWORK_OUTBOUND_CAPACITY_THRESHOLD = "network.outbound.capacity.threshold";

    private static final String LEADER_REPLICA_COUNT_BALANCE_THRESHOLD = "leader.replica.count.balance.threshold";

    private static final String AUTHENTICATION_METHOD = "auth_method";

    private static final String ADMIN_LEVEL_USERS = "auth_admins";

    private static final String TRUSTED_PROXY_SPNEGO_FALLBACK_ENABLED = "trusted.proxy.spnego.fallback.enabled";

    @Override
    public String getServiceType() {
        return CruiseControlRoles.CRUISECONTROL;
    }

    @Override
    public Set<String> getRoleTypes() {
        return Set.of(CruiseControlRoles.CRUISE_CONTROL_SERVER);
    }

    @Override
    public boolean sharedRoleType(String roleType) {
        return false;
    }

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        String cdpVersion = source.getBlueprintView().getProcessor().getStackVersion() == null ?
                "" : source.getBlueprintView().getProcessor().getStackVersion();

        if (isVersionNewerOrEqualThanLimited(cdpVersion, CLOUDERA_STACK_VERSION_7_2_11)) {
            return List.of(config(SELF_HEALING_ENABLED, "true"),
                    config(ANOMALY_NOTIFIER_CLASS, "com.linkedin.kafka.cruisecontrol.detector.notifier.SelfHealingNotifier"),
                    config(METRIC_ANOMALY_FINDER_CLASS, "com.cloudera.kafka.cruisecontrol.detector.EmptyBrokerAnomalyFinder"),
                    config(CPU_BALANCE_THRESHOLD, "1.5"),
                    config(DISK_BALANCE_THRESHOLD, "1.5"),
                    config(NETWORK_INBOUND_BALANCE_THRESHOLD, "1.5"),
                    config(NETWORK_OUTBOUND_BALANCE_THRESHOLD, "1.5"),
                    config(REPLICA_COUNT_BALANCE_THRESHOLD, "1.5"),
                    config(DISK_CAPACITY_THRESHOLD, "0.85"),
                    config(CPU_CAPACITY_THRESHOLD, "0.75"),
                    config(NETWORK_INBOUND_CAPACITY_THRESHOLD, "0.85"),
                    config(NETWORK_OUTBOUND_CAPACITY_THRESHOLD, "0.85"),
                    config(LEADER_REPLICA_COUNT_BALANCE_THRESHOLD, "1.5"),
                    config(ANOMALY_DETECTION_GOALS, "com.linkedin.kafka.cruisecontrol.analyzer.goals.RackAwareGoal," +
                            "com.linkedin.kafka.cruisecontrol.analyzer.goals.ReplicaCapacityGoal," +
                            "com.linkedin.kafka.cruisecontrol.analyzer.goals.DiskCapacityGoal"),
                    config(DEFAULT_GOALS, "com.linkedin.kafka.cruisecontrol.analyzer.goals.RackAwareGoal," +
                            "com.linkedin.kafka.cruisecontrol.analyzer.goals.ReplicaCapacityGoal," +
                            "com.linkedin.kafka.cruisecontrol.analyzer.goals.DiskCapacityGoal," +
                            "com.linkedin.kafka.cruisecontrol.analyzer.goals.NetworkInboundCapacityGoal," +
                            "com.linkedin.kafka.cruisecontrol.analyzer.goals.NetworkOutboundCapacityGoal," +
                            "com.linkedin.kafka.cruisecontrol.analyzer.goals.CpuCapacityGoal," +
                            "com.linkedin.kafka.cruisecontrol.analyzer.goals.ReplicaDistributionGoal," +
                            "com.linkedin.kafka.cruisecontrol.analyzer.goals.DiskUsageDistributionGoal," +
                            "com.linkedin.kafka.cruisecontrol.analyzer.goals.CpuUsageDistributionGoal," +
                            "com.linkedin.kafka.cruisecontrol.analyzer.goals.TopicReplicaDistributionGoal," +
                            "com.linkedin.kafka.cruisecontrol.analyzer.goals.LeaderReplicaDistributionGoal"),
                    config(HARD_GOALS, "com.linkedin.kafka.cruisecontrol.analyzer.goals.RackAwareGoal," +
                            "com.linkedin.kafka.cruisecontrol.analyzer.goals.ReplicaCapacityGoal," +
                            "com.linkedin.kafka.cruisecontrol.analyzer.goals.DiskCapacityGoal," +
                            "com.linkedin.kafka.cruisecontrol.analyzer.goals.NetworkInboundCapacityGoal," +
                            "com.linkedin.kafka.cruisecontrol.analyzer.goals.NetworkOutboundCapacityGoal," +
                            "com.linkedin.kafka.cruisecontrol.analyzer.goals.CpuCapacityGoal"),
                    config(AUTHENTICATION_METHOD, "Trusted Proxy"),
                    config(ADMIN_LEVEL_USERS, "kafka"),
                    config(TRUSTED_PROXY_SPNEGO_FALLBACK_ENABLED, "true")
            );
        }
        return List.of();
    }
}
