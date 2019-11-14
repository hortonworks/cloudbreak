package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static java.util.Collections.singletonMap;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.altus.AltusMachineUserService;
import com.sequenceiq.cloudbreak.telemetry.databus.DatabusConfigService;
import com.sequenceiq.cloudbreak.telemetry.databus.DatabusConfigView;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentConfigService;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentConfigView;
import com.sequenceiq.cloudbreak.telemetry.metering.MeteringConfigService;
import com.sequenceiq.cloudbreak.telemetry.metering.MeteringConfigView;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

/**
 * Decorate fluentd and metering related salt pillar configs (in order to ship data to cloud storage or databus)
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
 */
@Component
public class TelemetryDecorator {

    private final String version;

    private final DatabusConfigService databusConfigService;

    private final FluentConfigService fluentConfigService;

    private final MeteringConfigService meteringConfigService;

    private final AltusMachineUserService altusMachineUserService;

    public TelemetryDecorator(DatabusConfigService databusConfigService,
            FluentConfigService fluentConfigService,
            MeteringConfigService meteringConfigService,
            AltusMachineUserService altusMachineUserService,
            @Value("${info.app.version:}") String version) {
        this.databusConfigService = databusConfigService;
        this.fluentConfigService = fluentConfigService;
        this.meteringConfigService = meteringConfigService;
        this.altusMachineUserService = altusMachineUserService;
        this.version = version;
    }

    public Map<String, SaltPillarProperties> decoratePillar(Map<String, SaltPillarProperties> servicePillar,
            Stack stack, Telemetry telemetry) {
        Optional<AltusCredential> altusCredential = altusMachineUserService.generateDatabusMachineUserForFluent(stack, telemetry);
        String clusterType = StackType.DATALAKE.equals(stack.getType())
                ? FluentClusterType.DATALAKE.value() : FluentClusterType.DATAHUB.value();
        String serviceType = StackType.WORKLOAD.equals(stack.getType()) ? FluentClusterType.DATAHUB.value() : "";
        String accessKey = altusCredential.map(AltusCredential::getAccessKey).orElse(null);
        char[] privateKey = altusCredential.map(AltusCredential::getPrivateKey).orElse(null);

        DatabusConfigView databusConfigView = databusConfigService.createDatabusConfigs(accessKey, privateKey,
                null, telemetry.getDatabusEndpoint());
        if (databusConfigView.isEnabled()) {
            Map<String, Object> databusConfig = databusConfigView.toMap();
            servicePillar.put("databus",
                    new SaltPillarProperties("/databus/init.sls", singletonMap("databus", databusConfig)));
        }

        boolean meteringFeatureEnabled = telemetry.isMeteringFeatureEnabled();
        // for datalake - metering is not enabled yet
        boolean meteringEnabled = meteringFeatureEnabled && !StackType.DATALAKE.equals(stack.getType());

        FluentConfigView fluentConfigView = fluentConfigService.createFluentConfigs(clusterType,
                stack.getCloudPlatform(), databusConfigView.isEnabled(), meteringEnabled, telemetry);
        if (fluentConfigView.isEnabled()) {
            Map<String, Object> fluentConfig = fluentConfigView.toMap();
            servicePillar.put("fluent",
                    new SaltPillarProperties("/fluent/init.sls", singletonMap("fluent", fluentConfig)));
        }
        MeteringConfigView meteringConfigView = meteringConfigService.createMeteringConfigs(meteringEnabled,
                stack.getCloudPlatform(), stack.getResourceCrn(), serviceType, version);
        if (meteringConfigView.isEnabled()) {
            Map<String, Object> meteringConfig = meteringConfigView.toMap();
            servicePillar.put("metering",
                    new SaltPillarProperties("/metering/init.sls", singletonMap("metering", meteringConfig)));
        }
        return servicePillar;
    }
}