package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails.CLUSTER_CRN_KEY;
import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.altus.AltusMachineUserService;
import com.sequenceiq.cloudbreak.tag.ClusterTemplateApplicationTag;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
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
import com.sequenceiq.cloudbreak.telemetry.nodestatus.NodeStatusConfigService;
import com.sequenceiq.cloudbreak.telemetry.nodestatus.NodeStatusConfigView;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.MonitoringCredential;
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

    private final NodeStatusConfigService nodeStatusConfigService;

    private final TelemetryCommonConfigService telemetryCommonConfigService;

    private final AltusMachineUserService altusMachineUserService;

    private final VmLogsService vmLogsService;

    private final EntitlementService entitlementService;

    private final DataBusEndpointProvider dataBusEndpointProvider;

    public TelemetryDecorator(DatabusConfigService databusConfigService,
            FluentConfigService fluentConfigService,
            MeteringConfigService meteringConfigService,
            MonitoringConfigService monitoringConfigService,
            NodeStatusConfigService nodeStatusConfigService,
            TelemetryCommonConfigService telemetryCommonConfigService,
            AltusMachineUserService altusMachineUserService,
            VmLogsService vmLogsService,
            EntitlementService entitlementService,
            DataBusEndpointProvider dataBusEndpointProvider,
            @Value("${info.app.version:}") String version) {
        this.databusConfigService = databusConfigService;
        this.fluentConfigService = fluentConfigService;
        this.meteringConfigService = meteringConfigService;
        this.monitoringConfigService = monitoringConfigService;
        this.nodeStatusConfigService = nodeStatusConfigService;
        this.telemetryCommonConfigService = telemetryCommonConfigService;
        this.altusMachineUserService = altusMachineUserService;
        this.vmLogsService = vmLogsService;
        this.entitlementService = entitlementService;
        this.dataBusEndpointProvider = dataBusEndpointProvider;
        this.version = version;
    }

    public Map<String, SaltPillarProperties> decoratePillar(Map<String, SaltPillarProperties> servicePillar,
            StackView stack, ClusterView cluster, Telemetry telemetry, DataBusCredential dataBusCredential, MonitoringCredential monitoringCredential) {
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        AltusCredential dbusCredential = getAltusCredentialForDataBus(stack, accountId, telemetry, dataBusCredential);
        AltusCredential altusMonitoringCredential = getAltusCredentialForMonitoring(stack, accountId, telemetry, monitoringCredential);
        boolean useDbusCnameEndpoint = entitlementService.useDataBusCNameEndpointEnabled(accountId);
        String databusEndpoint = dataBusEndpointProvider.getDataBusEndpoint(telemetry.getDatabusEndpoint(), useDbusCnameEndpoint);
        String databusS3Endpoint = dataBusEndpointProvider.getDatabusS3Endpoint(databusEndpoint);

        DatabusConfigView databusConfigView = databusConfigService.createDatabusConfigs(
                dbusCredential.getAccessKey(), dbusCredential.getPrivateKey(), null, databusEndpoint);
        if (databusConfigView.isEnabled()) {
            addPillar(servicePillar, "databus", "/databus/init.sls", databusConfigView.toMap());
        }

        boolean datalakeCluster = StackType.DATALAKE.equals(stack.getType());
        boolean meteringFeatureEnabled = telemetry.isMeteringFeatureEnabled();
        // for datalake - metering is not enabled yet
        boolean meteringEnabled = meteringFeatureEnabled && !datalakeCluster;
        boolean databusEndpointValidationEnabled = !datalakeCluster && entitlementService.isDatahubDatabusEndpointValidationEnabled(accountId);
        String clusterCrn = datalakeCluster ? getDatalakeCrn(telemetry, stack.getResourceCrn()) : stack.getResourceCrn();
        final TelemetryClusterDetails clusterDetails = TelemetryClusterDetails.Builder.builder()
                .withOwner(stack.getCreator().getUserCrn())
                .withName(stack.getName())
                .withType(mapToFluentClusterType(stack.getType()))
                .withCrn(clusterCrn)
                .withPlatform(stack.getCloudPlatform())
                .withVersion(version)
                .withDatabusEndpoint(databusEndpoint)
                .withDatabusS3Endpoint(databusS3Endpoint)
                .withDatabusEndpointValidation(databusEndpointValidationEnabled)
                .build();
        final TelemetryCommonConfigView telemetryCommonConfigs = telemetryCommonConfigService.createTelemetryCommonConfigs(
                telemetry, vmLogsService.getVmLogs(), clusterDetails);
        addPillar(servicePillar, "telemetry", "/telemetry/init.sls", telemetryCommonConfigs.toMap());

        FluentConfigView fluentConfigView = fluentConfigService.createFluentConfigs(clusterDetails,
                databusConfigView.isEnabled(), meteringEnabled, stack.getRegion(), telemetry);
        if (fluentConfigView.isEnabled()) {
            addPillar(servicePillar, "fluent", "/fluent/init.sls", fluentConfigView.toMap());
        }
        setupMetering(servicePillar, stack, meteringEnabled);
        char[] nodePassword = getNodePassword(cluster);
        setupMonitoring(servicePillar, accountId, cluster, telemetry, nodePassword, Optional.of(altusMonitoringCredential));
        setupNodeStatusMonitor(servicePillar, stack, cluster, nodePassword);
        return servicePillar;
    }

    private AltusCredential getAltusCredentialForDataBus(StackView stack, String accountId, Telemetry telemetry, DataBusCredential dataBusCredential) {
        if (altusMachineUserService.isAnyDataBusBasedFeatureSupported(telemetry)) {
            if (needToGenerateCredential(accountId, telemetry, dataBusCredential)) {
                Optional<AltusCredential> altusCredential = altusMachineUserService.generateDatabusMachineUserForFluent(stack, telemetry);
                dataBusCredential = altusMachineUserService.storeDataBusCredential(altusCredential, stack);
            }
        }
        if (dataBusCredential == null || dataBusCredential.getAccessKey() == null || dataBusCredential.getPrivateKey() == null) {
            return new AltusCredential(null, null);
        } else {
            LOGGER.debug("Fill databus altus credential from DataBusCredential object...");
            return new AltusCredential(dataBusCredential.getAccessKey(), dataBusCredential.getPrivateKey().toCharArray());
        }
    }

    private AltusCredential getAltusCredentialForMonitoring(StackView stack, String accountId, Telemetry telemetry, MonitoringCredential credential) {
        if (altusMachineUserService.isAnyDataBusBasedFeatureSupported(telemetry)) {
            if (needToGenerateCredential(accountId, telemetry, credential)) {
                Optional<AltusCredential> altusCredential = altusMachineUserService.generateMonitoringMachineUser(stack, telemetry);
                credential = altusMachineUserService.storeMonitoringCredential(altusCredential, stack);
            }
        }
        if (credential == null || credential.getAccessKey() == null || credential.getPrivateKey() == null) {
            return new AltusCredential(null, null);
        } else {
            LOGGER.debug("Fill monitoring altus credential from MonitoringCredential object...");
            return new AltusCredential(credential.getAccessKey(), credential.getPrivateKey().toCharArray());
        }
    }

    private boolean needToGenerateCredential(String accountId, Telemetry telemetry, DataBusCredential credential) {
        if (credential == null || credential.getAccessKey() == null || credential.getPrivateKey() == null) {
            LOGGER.debug("No databus access/secret key is attached to the stack, generate new api key for machine user.");
            return true;
        } else if (altusMachineUserService.isCredentialExist(telemetry, accountId, credential.getMachineUserName(), credential.getAccessKey())) {
            LOGGER.debug("Databus credential data (machine user: {}; access key: {}) " +
                            "still exists on UMS side, won't generaete new ones",
                    credential.getMachineUserName(),
                    credential.getAccessKey());
            return false;
        } else {
            LOGGER.debug("Databus credential data (machine user: {}; access key: {}) " +
                            "does not exist on UMS side, generate new ones.",
                    credential.getMachineUserName(),
                    credential.getAccessKey());
            return true;
        }
    }

    private boolean needToGenerateCredential(String accountId, Telemetry telemetry, MonitoringCredential credential) {
        if (credential == null || credential.getAccessKey() == null || credential.getPrivateKey() == null) {
            LOGGER.debug("No monitoring access/secret key is attached to the stack, generate new api key for machine user.");
            return true;
        } else if (altusMachineUserService.isCredentialExist(telemetry, accountId, credential.getMachineUserName(), credential.getAccessKey())) {
            LOGGER.debug("Monitoring credential data (machine user: {}; access key: {}) " +
                            "still exists on UMS side, won't generate new ones",
                    credential.getMachineUserName(),
                    credential.getAccessKey());
            return false;
        } else {
            LOGGER.debug("Monitoring credential data (machine user: {}; access key: {}) " +
                            "does not exist on UMS side, generate new ones.",
                    credential.getMachineUserName(),
                    credential.getAccessKey());
            return true;
        }
    }

    private void setupMonitoring(Map<String, SaltPillarProperties> servicePillar, String accountId, ClusterView cluster,
            Telemetry telemetry, char[] nodePassword, Optional<AltusCredential> monitoringAccessKey) {
        boolean cdpSaasEnabled = entitlementService.isCdpSaasEnabled(accountId);
        boolean computeMonitoringEnabled = entitlementService.isComputeMonitoringEnabled(accountId);
        if (telemetry.getMonitoring() != null && telemetry.isMonitoringFeatureEnabled()) {
            LOGGER.debug("Filling monitoring configs.");
            MonitoringAuthConfig cmAuthConfig = null;
            if (cluster != null && cluster.getCloudbreakClusterManagerMonitoringUser() != null
                    && cluster.getCloudbreakClusterManagerMonitoringPassword() != null) {
                String cmMonitoringUser = cluster.getCloudbreakClusterManagerMonitoringUser();
                char[] cmMonitoringPassword = cluster.getCloudbreakClusterManagerMonitoringPassword().toCharArray();
                cmAuthConfig = new MonitoringAuthConfig(cmMonitoringUser, cmMonitoringPassword);
            }
            MonitoringConfigView monitoringConfigView = monitoringConfigService.createMonitoringConfig(telemetry.getMonitoring(),
                    MonitoringClusterType.CLOUDERA_MANAGER, cmAuthConfig, nodePassword, cdpSaasEnabled, computeMonitoringEnabled,
                    monitoringAccessKey.map(AltusCredential::getAccessKey).orElse(null),
                    monitoringAccessKey.map(AltusCredential::getPrivateKey).orElse(null),
                    null);
            if (monitoringConfigView.isEnabled()) {
                addPillar(servicePillar, "monitoring", "/monitoring/init.sls", monitoringConfigView.toMap());
            }
        } else {
            LOGGER.debug("CDP Saas is not enabled, do not use monitoring features");
        }
    }

    private void setupMetering(Map<String, SaltPillarProperties> servicePillar, StackView stack, boolean meteringEnabled) {
        if (meteringEnabled) {
            String defaultServiceType = StackType.WORKLOAD.equals(stack.getType()) ? FluentClusterType.DATAHUB.value() : "";
            MeteringConfigView meteringConfigView = meteringConfigService.createMeteringConfigs(meteringEnabled,
                    stack.getCloudPlatform(), stack.getResourceCrn(), stack.getName(), getServiceTypeForMetering(stack, defaultServiceType),
                    getVersionForMetering(stack, version));
            addPillar(servicePillar, "metering", "/metering/init.sls", meteringConfigView.toMap());
        }
    }

    private void setupNodeStatusMonitor(Map<String, SaltPillarProperties> servicePillar, StackView stack, ClusterView cluster, char[] passwordInput) {
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        boolean saltPingEnabled = entitlementService.nodestatusSaltPingEnabled(accountId);
        NodeStatusConfigView nodeStatusConfigView = nodeStatusConfigService
                .createNodeStatusConfig(cluster.getCdpNodeStatusMonitorUser(), passwordInput, saltPingEnabled);
        addPillar(servicePillar, "nodestatus", "/nodestatus/init.sls", nodeStatusConfigView.toMap());
    }

    private String getDatalakeCrn(Telemetry telemetry, String defaultClusterCrn) {
        String datalakeCrn = defaultClusterCrn;
        if (telemetry.getFluentAttributes() != null && telemetry.getFluentAttributes().containsKey(CLUSTER_CRN_KEY)
                && telemetry.getFluentAttributes().get(CLUSTER_CRN_KEY) != null) {
            datalakeCrn = telemetry.getFluentAttributes().get(CLUSTER_CRN_KEY).toString();
        }
        return datalakeCrn;
    }

    private String getServiceTypeForMetering(StackView stack, String defaultServiceType) {
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

    private String getVersionForMetering(StackView stack, String defaultVersion) {
        if (stack != null && StringUtils.isNotBlank(stack.getStackVersion())) {
            return stack.getStackVersion();
        } else {
            LOGGER.warn("No stack version for the cluster, use CB application version.");
            return defaultVersion;
        }
    }

    private void addPillar(Map<String, SaltPillarProperties> pillars, String name, String path, Map<String, Object> properties) {
        pillars.put(name, new SaltPillarProperties(path, singletonMap(name, properties)));
    }

    private char[] getNodePassword(ClusterView cluster) {
        if (StringUtils.isNotBlank(cluster.getCdpNodeStatusMonitorPassword())) {
            return cluster.getCdpNodeStatusMonitorPassword().toCharArray();
        } else {
            return null;
        }
    }

    private String mapToFluentClusterType(StackType stackType) {
        return StackType.DATALAKE.equals(stackType) ? FluentClusterType.DATALAKE.value() : FluentClusterType.DATAHUB.value();
    }
}