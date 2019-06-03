package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static java.util.Collections.singletonMap;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.Logging;
import com.sequenceiq.cloudbreak.cloud.model.LoggingOutputType;
import com.sequenceiq.cloudbreak.cloud.model.Telemetry;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

/**
 * Decorate fluentd related salt pillar configs (in order to ship daemon logs to cloud storage)
 * Currently only S3 output supported, right now salt properties are filled based on attributes,
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
 */
public class TelemetryDecorator {

    private static final String TD_AGENT_USER_DEFAULT = "root";

    private static final String TD_AGENT_GROUP_DEFAULT = "root";

    private static final Integer PARTITION_INTERVAL_MIN_DEFAULT = 5;

    private final Map<String, SaltPillarProperties> servicePillar;

    public TelemetryDecorator(Map<String, SaltPillarProperties> servicePillar) {
        this.servicePillar = servicePillar;
    }

    public void decoratePillar(Telemetry telemetry, String clusterName, StackType stackType) {
        if (telemetry != null) {
            Logging logging = telemetry.getLogging();
            if (logging != null && logging.isEnabled() && logging.getOutputType() != null) {
                if (logging.getAttributes() != null) {
                    Map<String, Object> fluentConfig = new HashMap<>();
                    fillFluentConfigs(logging, fluentConfig, clusterName, stackType);
                }
            }
        }
    }

    private void fillFluentConfigs(Logging logging, Map<String, Object> fluentConfig,
            String clusterName, StackType stackType) {
        Map<String, Object> attributes = logging.getAttributes();
        if (LoggingOutputType.S3.equals(logging.getOutputType())) {
            fluentConfig.put("enabled", true);
            fluentConfig.put("user", attributes.getOrDefault("user", TD_AGENT_USER_DEFAULT));
            fluentConfig.put("group", attributes.getOrDefault("group", TD_AGENT_GROUP_DEFAULT));
            fluentConfig.put("partitionIntervalMin", attributes.getOrDefault("partitionIntervalMin", PARTITION_INTERVAL_MIN_DEFAULT));
            fluentConfig.put("providerPrefix", "s3");
            fluentConfig.put("s3LogArchiveBucketName", attributes.get("s3LogArchiveBucketName"));
            String clusterType = "distrox";
            if (StackType.DATALAKE.equals(stackType)) {
                clusterType = "sdx";
            }
            String defaultLogFolder = Paths.get("cluster-logs", clusterType, clusterName).toString();
            fluentConfig.put("s3LogFolderName", attributes.getOrDefault("s3LogFolderName", defaultLogFolder));
            servicePillar.put("fluent",
                    new SaltPillarProperties("/fluent/init.sls", singletonMap("fluent", fluentConfig)));
        }
    }
}
