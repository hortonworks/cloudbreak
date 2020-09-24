package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails.CLUSTER_CRN_KEY;
import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.altus.AltusMachineUserService;
import com.sequenceiq.cloudbreak.tag.ClusterTemplateApplicationTag;
import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.VmLogsService;
import com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigService;
import com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigView;
import com.sequenceiq.cloudbreak.telemetry.databus.DatabusConfigService;
import com.sequenceiq.cloudbreak.telemetry.databus.DatabusConfigView;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentConfigService;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentConfigView;
import com.sequenceiq.cloudbreak.telemetry.metering.MeteringConfigService;
import com.sequenceiq.cloudbreak.telemetry.metering.MeteringConfigView;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringAuthConfig;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringClusterType;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfigService;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfigView;
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
public class TelemetryDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryDecorator.class);

    private final String version;

    private final DatabusConfigService databusConfigService;

    private final FluentConfigService fluentConfigService;

    private final MeteringConfigService meteringConfigService;

    private final MonitoringConfigService monitoringConfigService;

    private final TelemetryCommonConfigService telemetryCommonConfigService;

    private final AltusMachineUserService altusMachineUserService;

    private final VmLogsService vmLogsService;

    public TelemetryDecorator(DatabusConfigService databusConfigService,
            FluentConfigService fluentConfigService,
            MeteringConfigService meteringConfigService,
            MonitoringConfigService monitoringConfigService,
            TelemetryCommonConfigService telemetryCommonConfigService,
            AltusMachineUserService altusMachineUserService,
            VmLogsService vmLogsService,
            @Value("${info.app.version:}") String version) {
        this.databusConfigService = databusConfigService;
        this.fluentConfigService = fluentConfigService;
        this.meteringConfigService = meteringConfigService;
        this.monitoringConfigService = monitoringConfigService;
        this.telemetryCommonConfigService = telemetryCommonConfigService;
        this.altusMachineUserService = altusMachineUserService;
        this.vmLogsService = vmLogsService;
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

        boolean datalakeCluster = StackType.DATALAKE.equals(stack.getType());
        boolean meteringFeatureEnabled = telemetry.isMeteringFeatureEnabled();
        // for datalake - metering is not enabled yet
        boolean meteringEnabled = meteringFeatureEnabled && !datalakeCluster;
        String clusterCrn = datalakeCluster ? getDatalakeCrn(telemetry, stack.getResourceCrn()) : stack.getResourceCrn();
        final TelemetryClusterDetails clusterDetails = TelemetryClusterDetails.Builder.builder()
                .withOwner(stack.getCreator().getUserCrn())
                .withName(stack.getName())
                .withType(clusterType)
                .withCrn(clusterCrn)
                .withPlatform(stack.getCloudPlatform())
                .withVersion(version)
                .build();
        final TelemetryCommonConfigView telemetryCommonConfigs = telemetryCommonConfigService.createTelemetryCommonConfigs(
                telemetry, vmLogsService.getVmLogs(), clusterType, clusterCrn, stack.getName(), stack.getCreator().getUserCrn(), stack.getCloudPlatform());
        servicePillar.put("telemetry",
                new SaltPillarProperties("/telemetry/init.sls", Collections.singletonMap("telemetry", telemetryCommonConfigs.toMap())));

        FluentConfigView fluentConfigView = fluentConfigService.createFluentConfigs(clusterDetails,
                databusConfigView.isEnabled(), meteringEnabled, telemetry);
        if (fluentConfigView.isEnabled()) {
            Map<String, Object> fluentConfig = fluentConfigView.toMap();
            servicePillar.put("fluent",
                    new SaltPillarProperties("/fluent/init.sls", singletonMap("fluent", fluentConfig)));
        }
        setupMetering(servicePillar, stack, serviceType, meteringEnabled);
        setupMonitoring(servicePillar, stack, clusterDetails);
        return servicePillar;
    }

    private void setupMonitoring(Map<String, SaltPillarProperties> servicePillar, Stack stack, TelemetryClusterDetails clusterDetails) {
        if (stack.getCluster() != null && stack.getCluster().getCloudbreakClusterManagerMonitoringUser() != null
                && stack.getCluster().getCloudbreakClusterManagerMonitoringPassword() != null) {
            String monitoringUser = stack.getCluster().getCloudbreakClusterManagerMonitoringUser();
            char[] monitoringPassword = stack.getCluster().getCloudbreakClusterManagerMonitoringPassword().toCharArray();
            MonitoringAuthConfig authConfig = new MonitoringAuthConfig(monitoringUser, monitoringPassword);
            MonitoringConfigView monitoringConfigView = monitoringConfigService.createMonitoringConfig(
                    MonitoringClusterType.CLOUDERA_MANAGER, authConfig);
            if (monitoringConfigView.isEnabled()) {
                Map<String, Object> monitoringConfig = monitoringConfigView.toMap();
                servicePillar.put("monitoring",
                        new SaltPillarProperties("/monitoring/init.sls", singletonMap("monitoring", monitoringConfig)));
            }
        }
    }

    private void setupMetering(Map<String, SaltPillarProperties> servicePillar, Stack stack, String serviceType, boolean meteringEnabled) {
        MeteringConfigView meteringConfigView = meteringConfigService.createMeteringConfigs(meteringEnabled,
                stack.getCloudPlatform(), stack.getResourceCrn(), stack.getName(),
                getServiceTypeForMetering(stack, serviceType),
                getVersionForMetering(stack, version));
        if (meteringConfigView.isEnabled()) {
            Map<String, Object> meteringConfig = meteringConfigView.toMap();
            servicePillar.put("metering",
                    new SaltPillarProperties("/metering/init.sls", singletonMap("metering", meteringConfig)));
        }
    }

    private String getDatalakeCrn(Telemetry telemetry, String defaultClusterCrn) {
        String datalakeCrn = defaultClusterCrn;
        if (telemetry.getFluentAttributes() != null && telemetry.getFluentAttributes().containsKey(CLUSTER_CRN_KEY)
                && telemetry.getFluentAttributes().get(CLUSTER_CRN_KEY) != null) {
            datalakeCrn = telemetry.getFluentAttributes().get(CLUSTER_CRN_KEY).toString();
        }
        return datalakeCrn;
    }

    private String getServiceTypeForMetering(Stack stack, String defaultServiceType) {
        if (stack.getTags() != null) {
            try {
                StackTags stackTag = stack.getTags().get(StackTags.class);
                if (stackTag != null) {
                    Map<String, String> applicationTags = stackTag.getApplicationTags();
                    return applicationTags.getOrDefault(ClusterTemplateApplicationTag.SERVICE_TYPE.key(), defaultServiceType);
                }
            } catch (IOException e) {
                LOGGER.warn("Stack related applications tags cannot be parsed, use default service type for metering.", e);
            }
        }
        return defaultServiceType;
    }

    private String getVersionForMetering(Stack stack, String defaultVersion) {
        if (stack.getCluster() != null && stack.getCluster().getBlueprint() != null
                && StringUtils.isNotBlank(stack.getCluster().getBlueprint().getStackVersion())) {
            return stack.getCluster().getBlueprint().getStackVersion();
        } else {
            LOGGER.warn("No stack version for the cluster, use CB application version.");
            return defaultVersion;
        }
    }
}