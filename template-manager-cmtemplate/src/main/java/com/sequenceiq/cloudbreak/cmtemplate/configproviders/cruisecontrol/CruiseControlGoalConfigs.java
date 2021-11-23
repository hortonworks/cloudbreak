package com.sequenceiq.cloudbreak.cmtemplate.configproviders.cruisecontrol;

public class CruiseControlGoalConfigs {

    public static final String COMMON_DEFAULT_GOALS = "com.linkedin.kafka.cruisecontrol.analyzer.goals.ReplicaCapacityGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.DiskCapacityGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.NetworkInboundCapacityGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.NetworkOutboundCapacityGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.CpuCapacityGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.ReplicaDistributionGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.DiskUsageDistributionGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.CpuUsageDistributionGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.TopicReplicaDistributionGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.LeaderReplicaDistributionGoal";

    public static final String COMMON_HARD_GOALS = "com.linkedin.kafka.cruisecontrol.analyzer.goals.ReplicaCapacityGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.DiskCapacityGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.NetworkInboundCapacityGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.NetworkOutboundCapacityGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.CpuCapacityGoal";

    public static final String COMMON_ANOMALY_DETECTION_GOALS = "com.linkedin.kafka.cruisecontrol.analyzer.goals.ReplicaCapacityGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.DiskCapacityGoal";

    private CruiseControlGoalConfigs() { }
}
