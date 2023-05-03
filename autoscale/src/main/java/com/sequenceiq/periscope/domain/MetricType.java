package com.sequenceiq.periscope.domain;

import com.sequenceiq.cloudbreak.common.metrics.type.Metric;

public enum MetricType implements Metric {

    LEADER("node.leader"),
    CLUSTER_STATE_ACTIVE("cluster.state.active"),
    CLUSTER_STATE_SUSPENDED("cluster.state.suspended"),

    CLUSTER_UPSCALE_TRIGGERED("cluster.upscale.triggered"),
    CLUSTER_DOWNSCALE_TRIGGERED("cluster.downscale.triggered"),

    CLUSTER_UPSCALE_SUCCESSFUL("cluster.upscale.successful"),
    CLUSTER_UPSCALE_FAILED("cluster.upscale.failed"),
    CLUSTER_DOWNSCALE_SUCCESSFUL("cluster.downscale.successful"),
    CLUSTER_DOWNSCALE_FAILED("cluster.downscale.failed"),

    CLUSTER_MANAGER_API_INVOCATION("cluster.manager.api.invocation"),
    YARN_API_INVOCATION("yarn.api.invocation"),
    YARN_FORBIDDEN_EXCEPTION("yarn.forbidden.exception"),

    TOTAL_SCALING_ACTIVITIES("total.scaling.activities"),
    STALE_SCALING_ACTIVITY("stale.scaling.activity"),
    SCALING_ACTIVITY_CLEANUP_CANDIDATES("scaling.activity.cleanup.candidates"),

    IPA_USER_SYNC_INVOCATION("ipa.user.sync.invocation"),
    IPA_USER_SYNC_FAILED("ipa.user.sync.failed"),

    THREADPOOL_QUEUE_SIZE("threadpool.queue.size"),
    THREADPOOL_ACTIVE_THREADS("threadpool.threads.active"),
    THREADPOOL_THREADS_TOTAL("threadpool.threads.coresize"),
    THREADPOOL_TASKS_COMPLETED("threadpool.tasks.completed");

    private final String metricName;

    MetricType(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }
}
