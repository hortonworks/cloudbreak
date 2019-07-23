package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static java.util.Collections.singletonMap;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.fluent.FluentConfigService;
import com.sequenceiq.cloudbreak.fluent.FluentConfigView;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

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
@Component
public class TelemetryDecorator {

    private static final String CLUSTER_TYPE_DISTROX = "datahub";

    private static final String CLUSTER_TYPE_SDX = "datalake";

    private final FluentConfigService fluentConfigService;

    public TelemetryDecorator(FluentConfigService fluentConfigService) {
        this.fluentConfigService = fluentConfigService;
    }

    public Map<String, SaltPillarProperties> decoratePillar(Map<String, SaltPillarProperties> servicePillar,
            Stack stack, Telemetry telemetry) {
        String clusterType = StackType.DATALAKE.equals(stack.getType()) ? CLUSTER_TYPE_SDX : CLUSTER_TYPE_DISTROX;
        FluentConfigView fluentConfigView = fluentConfigService.createFluentConfigs(stack.getCluster().getName(),
                clusterType, stack.getCloudPlatform(),  telemetry);
        if (fluentConfigView.isEnabled()) {
            Map<String, Object> fluentConfig = fluentConfigView.toMap();
            servicePillar.put("fluent",
                    new SaltPillarProperties("/fluent/init.sls", singletonMap("fluent", fluentConfig)));
        }
        return servicePillar;
    }
}