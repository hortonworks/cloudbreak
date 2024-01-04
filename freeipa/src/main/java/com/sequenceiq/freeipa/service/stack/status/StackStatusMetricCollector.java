package com.sequenceiq.freeipa.service.stack.status;

import static com.sequenceiq.freeipa.metrics.FreeIpaMetricTag.CLOUD_PLATFORM;
import static com.sequenceiq.freeipa.metrics.FreeIpaMetricTag.STATUS;
import static com.sequenceiq.freeipa.metrics.FreeIpaMetricTag.TUNNEL;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.metrics.status.StackCountByStatusAndCloudPlatform;
import com.sequenceiq.cloudbreak.common.metrics.status.StackCountByStatusAndTunnel;
import com.sequenceiq.cloudbreak.common.metrics.status.StatusMetricCollector;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.metrics.MetricType;
import com.sequenceiq.freeipa.service.stack.StackStatusService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;

@Component
@ConditionalOnProperty(name = "status.metrics.collector.enabled", havingValue = "true", matchIfMissing = true)
public class StackStatusMetricCollector implements StatusMetricCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStatusMetricCollector.class);

    private static final EnumSet<CloudPlatform> CLOUD_PLATFORMS = EnumSet.of(CloudPlatform.AWS, CloudPlatform.AZURE, CloudPlatform.GCP);

    private MultiGauge stackStatusByCloudPlatformMultiGauge;

    private MultiGauge stackStatusByTunnelMultiGauge;

    @Inject
    private MeterRegistry meterRegistry;

    @Inject
    private StackStatusService stackStatusService;

    @PostConstruct
    public void init() {
        stackStatusByCloudPlatformMultiGauge = MultiGauge.builder(MetricType.STACK_STATUS_CLOUDPLATFORM_COUNT.getMetricName()).register(meterRegistry);
        stackStatusByTunnelMultiGauge = MultiGauge.builder(MetricType.STACK_STATUS_TUNNEL_COUNT.getMetricName()).register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${status.metrics.collector.delay:30000}")
    @Override
    public void collectStatusMetrics() {
        List<StackCountByStatusAndCloudPlatform> stackCountByStatusAndCloudPlatform = CLOUD_PLATFORMS.stream()
                .flatMap(cloudPlatform -> stackStatusService.countStacksByStatusAndCloudPlatform(cloudPlatform.name()).stream()
                        .map(stackCountByStatus -> new StackCountByStatusAndCloudPlatform(stackCountByStatus, cloudPlatform)))
                .toList();
        stackStatusByCloudPlatformMultiGauge.register(stackCountByStatusAndCloudPlatform.stream()
                .map(count -> MultiGauge.Row.of(Tags.of(CLOUD_PLATFORM.name(),
                        count.cloudPlatform().name(), STATUS.name(),
                        count.status()), count.count()))
                .collect(Collectors.toList()), true);

        List<StackCountByStatusAndTunnel> stackCountByStatusAndTunnel = Arrays.asList(Tunnel.values()).stream()
                .flatMap(tunnel -> stackStatusService.countStacksByStatusAndTunnel(tunnel).stream()
                        .map(stackCountByStatus -> new StackCountByStatusAndTunnel(stackCountByStatus, tunnel)))
                .toList();
        stackStatusByTunnelMultiGauge.register(stackCountByStatusAndTunnel.stream()
                .map(count -> MultiGauge.Row.of(Tags.of(TUNNEL.name(),
                        count.tunnel().name(), STATUS.name(),
                        count.status()), count.count()))
                .collect(Collectors.toList()), true);
    }
}
