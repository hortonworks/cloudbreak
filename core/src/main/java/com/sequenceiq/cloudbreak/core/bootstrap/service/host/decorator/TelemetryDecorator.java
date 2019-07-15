package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static java.util.Collections.singletonMap;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.fluent.FluentConfigView;
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
@Component
public class TelemetryDecorator {

    public Map<String, SaltPillarProperties> decoratePillar(Map<String, SaltPillarProperties> servicePillar, FluentConfigView fluentConfigView) {
        if (fluentConfigView.isEnabled()) {
            Map<String, Object> fluentConfig = fluentConfigView.toMap();
            Map<String, Object> overrideAttributes = fluentConfigView.getOverrideAttributes();
            if (overrideAttributes != null) {
                for (Map.Entry<String, Object> entry : overrideAttributes.entrySet()) {
                    if (fluentConfig.containsKey(entry.getKey())) {
                        fluentConfig.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            servicePillar.put("fluent",
                    new SaltPillarProperties("/fluent/init.sls", singletonMap("fluent", fluentConfig)));
        }
        return servicePillar;
    }
}