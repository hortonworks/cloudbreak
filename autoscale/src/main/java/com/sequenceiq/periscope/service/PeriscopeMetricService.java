package com.sequenceiq.periscope.service;

import static com.sequenceiq.periscope.domain.MetricType.CLUSTER_MANAGER_API_INVOCATION;
import static com.sequenceiq.periscope.domain.MetricType.SCALING_ACTIVITY_DURATION;
import static com.sequenceiq.periscope.domain.MetricType.YARN_API_INVOCATION;
import static java.time.Instant.now;
import static java.util.Objects.isNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.metrics.AbstractMetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.Metric;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricTag;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricType;

@Primary
@Service("MetricService")
public class PeriscopeMetricService extends AbstractMetricService {

    private static final String METRIC_PREFIX = "periscope";

    private static final Set<String> GAUGE_METRICS = Sets.newHashSet("state", "leader", "threadpool", "scaling.activity");

    @PostConstruct
    protected void init() {
        Arrays.stream(MetricType.values())
                .filter(m -> !gaugeMetric(m))
                .forEach(this::initMicrometerMetricCounter);

        Arrays.stream(MetricType.values())
                .filter(this::gaugeMetric)
                .forEach(m -> gauge(m, 0));
    }

    @Override
    protected Optional<String> getMetricPrefix() {
        return Optional.of(METRIC_PREFIX);
    }

    @Override
    protected boolean gaugeMetric(Metric metric) {
        return GAUGE_METRICS.stream().anyMatch(m -> metric.getMetricName().contains(m));
    }

    private String[] createTags(Cluster cluster) {
        return new String[]{MetricTag.TENANT.name(), extractTenant(cluster),
                MetricTag.CLOUD_PROVIDER.name(), Optional.ofNullable(cluster.getCloudPlatform()).orElse("NA"),
        };
    }

    private String extractTenant(Cluster cluster) {
        return isNull(cluster.getClusterPertain()) ? "NA" : Optional.ofNullable(cluster.getClusterPertain().getTenant()).orElse("NA");
    }

    public void recordClusterManagerInvocation(Cluster cluster, long startMillis) {
        long duration = now().toEpochMilli() - startMillis;
        recordTimer(duration, CLUSTER_MANAGER_API_INVOCATION, createTags(cluster));
    }

    public void recordYarnInvocation(Cluster cluster, long startMillis) {
        long duration = now().toEpochMilli() - startMillis;
        recordTimer(duration, YARN_API_INVOCATION, createTags(cluster));
    }

    public void recordScalingAtivityDuration(Cluster cluster, long startMillis) {
        long duration = now().toEpochMilli() - startMillis;
        recordTimer(duration, SCALING_ACTIVITY_DURATION, createTags(cluster));
    }
}
