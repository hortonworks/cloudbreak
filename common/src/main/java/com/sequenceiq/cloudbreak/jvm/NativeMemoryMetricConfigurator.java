package com.sequenceiq.cloudbreak.jvm;

import static com.sequenceiq.cloudbreak.jvm.JvmMetricTag.CATEGORY;
import static com.sequenceiq.cloudbreak.jvm.JvmMetricTag.TYPE;
import static com.sequenceiq.cloudbreak.jvm.JvmMetricType.JVM_NATIVE_MEMORY;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.util.Benchmark;

import io.github.mweirauch.micrometer.jvm.extras.ProcessMemoryMetrics;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class NativeMemoryMetricConfigurator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeMemoryMetricConfigurator.class);

    private static final String NATIVE_MEMORY_JVM_ARGUMENT = "-XX:NativeMemoryTracking=summary";

    private static final String COMMITTED = "committed";

    private static final String RESERVED = "reserved";

    @Value("${vm.info.nativeMemory.enabled:true}")
    private boolean nativeMemoryInfo;

    @Qualifier("CommonMetricService")
    @Inject
    private MetricService metricService;

    @Inject
    private MeterRegistry meterRegistry;

    @Inject
    private DiagnosticCommandRunner diagnosticCommandRunner;

    @Inject
    private NativeMemoryParser nativeMemoryParser;

    private boolean nativeMemoryMetricsEnabled;

    @PostConstruct
    public void init() {
        if (nativeMemoryInfo) {
            checkNativeMemoryJvmArgument();
            enableProcMetrics();
        }
    }

    @Scheduled(fixedDelayString = "${vm.info.nativeMemory.delay:60000}")
    public void calculateNativeMemoryMetrics() {
        if (nativeMemoryMetricsEnabled) {
            Benchmark.measure(() -> calculateMetrics(), LOGGER, "Native memory metric calculation took {} ms.");
        }
    }

    private void checkNativeMemoryJvmArgument() {
        try {
            RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
            Optional<String> nativeMemoryJvmArgument = runtimeMxBean.getInputArguments().stream()
                    .filter(argument -> argument.equals(NATIVE_MEMORY_JVM_ARGUMENT))
                    .findAny();
            if (nativeMemoryJvmArgument.isPresent()) {
                nativeMemoryMetricsEnabled = true;
            }
        } catch (Exception e) {
            LOGGER.error("Native memory metric initialization failed", e);
        }
    }

    private void calculateMetrics() {
        try {
            String vmNativeMemory = diagnosticCommandRunner.vmNativeMemory();
            List<MemoryCategory> vmNativeMemoryCategories = nativeMemoryParser.parseVmNativeMemory(vmNativeMemory);
            vmNativeMemoryCategories.stream().forEach(category -> {
                metricService.gauge(JVM_NATIVE_MEMORY, category.committed(), Map.of(CATEGORY.name(), category.name(), TYPE.name(), COMMITTED));
                metricService.gauge(JVM_NATIVE_MEMORY, category.reserved(), Map.of(CATEGORY.name(), category.name(), TYPE.name(), RESERVED));
            });
        } catch (Exception e) {
            LOGGER.error("Native memory metric calculation failed", e);
        }
    }

    private void enableProcMetrics() {
        if (SystemUtils.IS_OS_LINUX) {
            new ProcessMemoryMetrics().bindTo(meterRegistry);
        }
    }
}
