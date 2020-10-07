package com.sequenceiq.cloudbreak.service.metrics;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.AbstractMetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricTag;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;

@Service("MetricService")
public class CloudbreakMetricService extends AbstractMetricService {

    private static final String METRIC_PREFIX = "cloudbreak";

    public void incrementMetricCounter(MetricType metric, Stack stack) {
        String[] tags = nullableValueTags(
                CloudbreakMetricTag.STACK_TYPE.name(), stack.getType().name(),
                MetricTag.CLOUD_PROVIDER.name(), stack.cloudPlatform(),
                MetricTag.TUNNEL_TYPE.name(), stack.getTunnel().name());
        incrementMetricCounter(metric, tags);
    }

    public void incrementMetricCounter(MetricType metric, Stack stack, Exception e) {
        String[] tags = nullableValueTags(
                MetricTag.EXCEPTION_TYPE.name(), e.getClass().getName(),
                CloudbreakMetricTag.STACK_TYPE.name(), stack.getType().name(),
                MetricTag.CLOUD_PROVIDER.name(), stack.cloudPlatform(),
                MetricTag.TUNNEL_TYPE.name(), stack.getTunnel().name());
        incrementMetricCounter(metric, tags);
    }

    public void incrementMetricCounter(MetricType metric, StackView stack) {
        String[] tags = nullableValueTags(
                CloudbreakMetricTag.STACK_TYPE.name(), stack.getType().name(),
                MetricTag.CLOUD_PROVIDER.name(), stack.cloudPlatform(),
                MetricTag.TUNNEL_TYPE.name(), stack.getTunnel().name());
        incrementMetricCounter(metric, tags);
    }

    public void incrementMetricCounter(MetricType metric, StackView stack, Exception e) {
        String[] tags = nullableValueTags(
                MetricTag.EXCEPTION_TYPE.name(), e.getClass().getName(),
                CloudbreakMetricTag.STACK_TYPE.name(), stack.getType().name(),
                MetricTag.CLOUD_PROVIDER.name(), stack.cloudPlatform(),
                MetricTag.TUNNEL_TYPE.name(), stack.getTunnel().name());
        incrementMetricCounter(metric, tags);
    }

    @Override
    protected String getMetricPrefix() {
        return METRIC_PREFIX;
    }

    private String[] nullableValueTags(String... tags) {
        if (tags.length % 2 == 1) {
            throw new IllegalArgumentException("tags size must be even, it is a set of key=value pairs of tags");
        }
        ArrayList<String> accumulatedTags = new ArrayList<>();
        for (int i = 0; i < tags.length; i += 2) {
            if (tags[i + 1] != null) {
                accumulatedTags.add(tags[i]);
                accumulatedTags.add(tags[i + 1]);
            }
        }
        String[] arrAccumulatedTags = new String[accumulatedTags.size()];
        arrAccumulatedTags = accumulatedTags.toArray(arrAccumulatedTags);
        return arrAccumulatedTags;
    }

    public void recordImageCopyTime(Stack stack, long startMillis) {
        String[] tags = {MetricTag.TENANT.name(), Optional.ofNullable(stack.getTenant()).map(Tenant::getName).orElse("NA"),
                MetricTag.CLOUD_PROVIDER.name(), Optional.ofNullable(stack.cloudPlatform()).orElse("NA"),
                MetricTag.REGION.name(), Optional.ofNullable(stack.getRegion()).orElse("NA")};

        long millispassed = System.currentTimeMillis() - startMillis;
        recordTimer(millispassed, MetricType.STACK_IMAGE_COPY, tags);
    }
}
