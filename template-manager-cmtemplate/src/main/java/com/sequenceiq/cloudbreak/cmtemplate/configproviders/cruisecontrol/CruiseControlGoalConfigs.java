package com.sequenceiq.cloudbreak.cmtemplate.configproviders.cruisecontrol;

public class CruiseControlGoalConfigs {

    public static final String COMMON_SUPPORTED_GOALS = "com.linkedin.kafka.cruisecontrol.analyzer.goals.RackAwareDistributionGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.ReplicaCapacityGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.DiskCapacityGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.NetworkInboundCapacityGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.NetworkOutboundCapacityGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.CpuCapacityGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.ReplicaDistributionGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.PotentialNwOutGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.DiskUsageDistributionGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.NetworkInboundUsageDistributionGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.NetworkOutboundUsageDistributionGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.CpuUsageDistributionGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.TopicReplicaDistributionGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.LeaderReplicaDistributionGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.LeaderBytesInDistributionGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.goals.PreferredLeaderElectionGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.kafkaassigner.KafkaAssignerDiskUsageDistributionGoal," +
            "com.linkedin.kafka.cruisecontrol.analyzer.kafkaassigner.KafkaAssignerEvenRackAwareGoal";

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
