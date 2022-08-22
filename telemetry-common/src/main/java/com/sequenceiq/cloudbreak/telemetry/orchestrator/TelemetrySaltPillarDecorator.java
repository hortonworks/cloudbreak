package com.sequenceiq.cloudbreak.telemetry.orchestrator;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.orchestration.OrchestratorAware;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfigView;
import com.sequenceiq.cloudbreak.telemetry.TelemetryContextProvider;
import com.sequenceiq.cloudbreak.telemetry.TelemetryPillarConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;

/**
 * Decorate telemetry related (fluentd,metering,monitoring,databus) salt pillar configs (in order to ship data to cloud storage or databus)
 * Currently only S3/WASB cloud storage output supported, right now salt properties are filled based on attributes,
 * the calculation can be changed based on UI requirements.
 * The defaults could look like this:
 * <pre>
 * fluent:
 *   enabled: false
 *   user: root
 *   group: root
 *   providerPrefix: "stdout"
 *   partitionIntervalMin: 5
 *   s3LogArchiveBucketName:
 *   s3LogFolderName:
 * </pre>
 * Or for metering:
 * <pre>
 * metering:
 *   enabled: true
 *   serviceType: DATAHUB
 *   serviceVersion: 2.11.2
 *   cluserCrn: crn:mycluster:1111...
 * </pre>
 * Or for monitoring:
 * <pre>
 * monitoring:
 *   enabled: true
 *   type: cloudera_manager
 *   clusterType: DATAHUB
 *   clusterVersion: 2.11.2
 *   clusterCrn: crn:mycluster:1111...
 * </pre>
 */
@Component
public class TelemetrySaltPillarDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetrySaltPillarDecorator.class);

    private final TelemetryContextProvider telemetryContextProvider;

    private final List<? extends TelemetryPillarConfigGenerator<? extends TelemetryConfigView>> generators;

    public TelemetrySaltPillarDecorator(TelemetryContextProvider telemetryContextProvider,
            List<? extends TelemetryPillarConfigGenerator<? extends TelemetryConfigView>> generators) {
        this.telemetryContextProvider = telemetryContextProvider;
        this.generators = generators;
    }

    public Map<String, SaltPillarProperties> generatePillarConfigMap(OrchestratorAware stack) {
        Map<String, Map<String, Map<String, Object>>> telemetryPillarsMap = new HashMap<>();
        TelemetryContext telemetryContext = telemetryContextProvider.createTelemetryContext(stack);
        LOGGER.debug("Telemetry context: {}", telemetryContext);
        for (TelemetryPillarConfigGenerator<? extends TelemetryConfigView> pillarConfigGenerator : generators) {
            if (pillarConfigGenerator.isEnabled(telemetryContext)) {
                LOGGER.debug("Telemetry decoration is enabled for {}", pillarConfigGenerator.getClass().getCanonicalName());
                TelemetryConfigView configView = pillarConfigGenerator.createConfigs(telemetryContext);
                telemetryContext.addConfigView(configView);
                telemetryPillarsMap.putAll(pillarConfigGenerator.getSaltPillars(configView, telemetryContext));
            } else {
                LOGGER.debug("Telemetry decoration is disabled for {}", pillarConfigGenerator.getClass().getCanonicalName());
            }
        }
        return telemetryPillarsMap.entrySet().stream().map(entry -> {
            SaltPillarProperties pillarProperties = entry.getValue().entrySet()
                    .stream().map(e -> new SaltPillarProperties(e.getKey(), e.getValue())).findFirst().orElse(null);
            return new AbstractMap.SimpleEntry<>(entry.getKey(), pillarProperties);
        }).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }
}
