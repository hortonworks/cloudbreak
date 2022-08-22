package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails.CLUSTER_CRN_KEY;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.lang3.ObjectUtils;
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
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.altus.AltusMachineUserService;
import com.sequenceiq.cloudbreak.tag.ClusterTemplateApplicationTag;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.TelemetryContextProvider;
import com.sequenceiq.cloudbreak.telemetry.VmLogsService;
import com.sequenceiq.cloudbreak.telemetry.context.DatabusContext;
import com.sequenceiq.cloudbreak.telemetry.context.LogShipperContext;
import com.sequenceiq.cloudbreak.telemetry.context.MeteringContext;
import com.sequenceiq.cloudbreak.telemetry.context.MonitoringContext;
import com.sequenceiq.cloudbreak.telemetry.context.NodeStatusContext;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringAuthConfig;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringClusterType;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfigService;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringServiceType;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.telemetry.model.CdpCredential;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.MonitoringCredential;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.VmLog;

@Component
public class TelemetryDecorator implements TelemetryContextProvider<StackDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryDecorator.class);

    private final String version;

    private final AltusMachineUserService altusMachineUserService;

    private final VmLogsService vmLogsService;

    private final EntitlementService entitlementService;

    private final DataBusEndpointProvider dataBusEndpointProvider;

    private final MonitoringConfigService monitoringConfigService;

    private final ComponentConfigProviderService componentConfigProviderService;

    public TelemetryDecorator(AltusMachineUserService altusMachineUserService,
            VmLogsService vmLogsService,
            EntitlementService entitlementService,
            DataBusEndpointProvider dataBusEndpointProvider,
            MonitoringConfigService monitoringConfigService,
            ComponentConfigProviderService componentConfigProviderService,
            @Value("${info.app.version:}") String version) {
        this.altusMachineUserService = altusMachineUserService;
        this.vmLogsService = vmLogsService;
        this.entitlementService = entitlementService;
        this.dataBusEndpointProvider = dataBusEndpointProvider;
        this.monitoringConfigService = monitoringConfigService;
        this.componentConfigProviderService = componentConfigProviderService;
        this.version = version;
    }

    @Override
    public TelemetryContext createTelemetryContext(StackDto stackDto) {
        StackView stack = stackDto.getStack();
        ClusterView cluster = stackDto.getCluster();
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        Telemetry telemetry = componentConfigProviderService.getTelemetry(stack.getId());
        DataBusCredential dataBusCredential = convertOrReturnNull(cluster.getDatabusCredential(), DataBusCredential.class);
        MonitoringCredential monitoringCredential = convertOrReturnNull(cluster.getMonitoringCredential(), MonitoringCredential.class);
        TelemetryContext telemetryContext = new TelemetryContext();
        telemetryContext.setClusterType(mapToFluentClusterType(stack.getType()));
        telemetryContext.setTelemetry(telemetry);
        if (telemetry != null) {
            DatabusContext databusContext = createDatabusContext(stack, telemetry, dataBusCredential, accountId);
            telemetryContext.setDatabusContext(databusContext);
            telemetryContext.setClusterDetails(createTelemetryClusterDetails(stack, telemetry, databusContext));
            telemetryContext.setMeteringContext(createMeteringContext(stack, telemetry));
            NodeStatusContext nodeStatusContext = createNodeStatusContext(cluster, accountId);
            telemetryContext.setNodeStatusContext(nodeStatusContext);
            telemetryContext.setLogShipperContext(createLogShipperContext(stack, telemetry));
            telemetryContext.setMonitoringContext(createMonitoringContext(stack, cluster, telemetryContext, accountId, monitoringCredential));
        }
        return telemetryContext;
    }

    private DatabusContext createDatabusContext(StackView stack, Telemetry telemetry, DataBusCredential dataBusCredential, String accountId) {
        boolean useDbusCnameEndpoint = entitlementService.useDataBusCNameEndpointEnabled(accountId);
        String databusEndpoint = dataBusEndpointProvider.getDataBusEndpoint(telemetry.getDatabusEndpoint(), useDbusCnameEndpoint);
        String databusS3Endpoint = dataBusEndpointProvider.getDatabusS3Endpoint(databusEndpoint);
        DatabusContext.Builder builder = DatabusContext.builder();
        AltusCredential dbusCredential = getAltusCredentialForDataBus(stack, accountId, telemetry, dataBusCredential);
        if (telemetry.isAnyDataBusBasedFeatureEnablred()) {
            builder.enabled();
        }
        if (!dbusCredential.isEmpty()) {
            DataBusCredential credential = new DataBusCredential();
            credential.setAccessKey(dbusCredential.getAccessKey());
            credential.setPrivateKey(new String(dbusCredential.getPrivateKey()));
            builder.withCredential(credential);
        }
        boolean datalakeCluster = StackType.DATALAKE.equals(stack.getType());
        if (!datalakeCluster && entitlementService.isDatahubDatabusEndpointValidationEnabled(accountId)) {
            builder.withValidation();
        }
        return builder
                .withEndpoint(databusEndpoint)
                .withS3Endpoint(databusS3Endpoint)
                .build();
    }

    private MonitoringContext createMonitoringContext(StackView stack, ClusterView cluster, TelemetryContext telemetryContext,
            String accountId, MonitoringCredential monitoringCredential) {
        MonitoringContext.Builder builder = MonitoringContext.builder();
        Telemetry telemetry = telemetryContext.getTelemetry();
        NodeStatusContext nodeStatusContext = telemetryContext.getNodeStatusContext();
        boolean cdpSaasEnabled = entitlementService.isCdpSaasEnabled(accountId);
        boolean computeMonitoring = entitlementService.isComputeMonitoringEnabled(accountId);
        boolean monitoringEnabled = monitoringConfigService.isMonitoringEnabled(cdpSaasEnabled, computeMonitoring);
        builder.withClusterType(MonitoringClusterType.CLOUDERA_MANAGER);
        if (monitoringEnabled && telemetry.isComputeMonitoringEnabled()) {
            builder.enabled();
            AltusCredential altusCred = getAltusCredentialForMonitoring(stack, accountId, telemetry, monitoringCredential);
            MonitoringCredential credential = new MonitoringCredential();
            credential.setAccessKey(altusCred.getAccessKey());
            credential.setPrivateKey(new String(altusCred.getPrivateKey()));
            builder.withCredential(credential);
            MonitoringAuthConfig cmAuthConfig = null;
            if (cluster != null && cluster.getCloudbreakClusterManagerMonitoringUser() != null
                    && cluster.getCloudbreakClusterManagerMonitoringPassword() != null) {
                String cmMonitoringUser = cluster.getCloudbreakClusterManagerMonitoringUser();
                char[] cmMonitoringPassword = cluster.getCloudbreakClusterManagerMonitoringPassword().toCharArray();
                cmAuthConfig = new MonitoringAuthConfig(cmMonitoringUser, cmMonitoringPassword);
                builder.withCmAutoTls(cluster.getAutoTlsEnabled());
            }
            builder.withCmAuth(cmAuthConfig);
        }
        if (cdpSaasEnabled) {
            builder.withServiceType(MonitoringServiceType.SAAS);
        }
        builder.withSharedPassword(nodeStatusContext.getPassword());
        if (telemetry.getMonitoring() != null) {
            builder.withRemoteWriteUrl(telemetry.getMonitoring().getRemoteWriteUrl());
        }
        return builder.build();
    }

    private MeteringContext createMeteringContext(StackView stack, Telemetry telemetry) {
        MeteringContext.Builder builder = MeteringContext.builder();
        boolean datahub = !StackType.DATALAKE.equals(stack.getType());
        boolean meteringFeatureEnabled = telemetry.isMeteringFeatureEnabled();
        boolean meteringEnabled = meteringFeatureEnabled && datahub;
        if (meteringEnabled) {
            builder.enabled();
        }
        String defaultServiceType = StackType.WORKLOAD.equals(stack.getType()) ? FluentClusterType.DATAHUB.value() : "";
        return builder
                .withServiceType(getServiceTypeForMetering(stack, defaultServiceType))
                .withVersion(getVersionForMetering(stack, version))
                .build();
    }

    private LogShipperContext createLogShipperContext(StackView stack, Telemetry telemetry) {
        LogShipperContext.Builder builder = LogShipperContext.builder();
        List<VmLog> vmLogList = vmLogsService.getVmLogs();
        Logging logging = telemetry.getLogging();
        if (telemetry.isCloudStorageLoggingEnabled() && logging != null
                && ObjectUtils.anyNotNull(logging.getS3(), logging.getAdlsGen2(), logging.getGcs(), logging.getCloudwatch())) {
            builder.enabled().cloudStorageLogging();
        }
        if (telemetry.isClusterLogsCollectionEnabled()) {
            builder.enabled().collectDeploymentLogs();
        }
        return builder
                .withVmLogs(vmLogList)
                .withCloudRegion(stack.getRegion())
                .build();
    }

    private NodeStatusContext createNodeStatusContext(ClusterView cluster, String accountId) {
        NodeStatusContext.Builder builder = NodeStatusContext.builder();
        boolean saltPingEnabled = entitlementService.nodestatusSaltPingEnabled(accountId);
        if (saltPingEnabled) {
            builder.saltPingEnabled();
        }
        if (StringUtils.isNotBlank(cluster.getCdpNodeStatusMonitorPassword())) {
            builder.withPassword(cluster.getCdpNodeStatusMonitorPassword().toCharArray());
        }
        return builder
                .withUsername(cluster.getCdpNodeStatusMonitorUser())
                .build();
    }

    private TelemetryClusterDetails createTelemetryClusterDetails(StackView stack, Telemetry telemetry, DatabusContext databusContext) {
        String clusterCrn = StackType.DATALAKE.equals(stack.getType()) ? getDatalakeCrn(telemetry, stack.getResourceCrn()) : stack.getResourceCrn();
        return TelemetryClusterDetails.Builder.builder()
                .withOwner(stack.getCreator().getUserCrn())
                .withName(stack.getName())
                .withType(mapToFluentClusterType(stack.getType()).value())
                .withCrn(clusterCrn)
                .withPlatform(stack.getCloudPlatform())
                .withVersion(version)
                .withDatabusEndpoint(databusContext.getEndpoint())
                .withDatabusS3Endpoint(databusContext.getS3Endpoint())
                .withDatabusEndpointValidation(databusContext.isValidation())
                .build();
    }

    private AltusCredential getAltusCredentialForDataBus(StackView stack, String accountId, Telemetry telemetry, DataBusCredential dataBusCredential) {
        return getAltusCredential(accountId, telemetry, dataBusCredential, "DataBus", altusMachineUserService::isAnyDataBusBasedFeatureSupported,
                () -> {
                    Optional<AltusCredential> altusCredential = altusMachineUserService.generateDatabusMachineUserForFluent(stack, telemetry);
                    return altusMachineUserService.storeDataBusCredential(altusCredential, stack);
                });
    }

    private AltusCredential getAltusCredentialForMonitoring(StackView stack, String accountId, Telemetry telemetry, MonitoringCredential credential) {
        return getAltusCredential(accountId, telemetry, credential, "Monitoring", altusMachineUserService::isAnyMonitoringFeatureSupported,
                () -> {
                    Optional<AltusCredential> altusCredential = altusMachineUserService.generateMonitoringMachineUser(stack, telemetry);
                    return altusMachineUserService.storeMonitoringCredential(altusCredential, stack);
                });
    }

    private <C extends CdpCredential> AltusCredential getAltusCredential(String accountId, Telemetry telemetry,
            C credential, String type, Predicate<Telemetry> featureSupportedFunc, Supplier<C> credGenerator) {
        if (featureSupportedFunc.test(telemetry)) {
            if (needToGenerateCredential(accountId, telemetry, credential, type)) {
                credential = credGenerator.get();
            }
        }
        if (credential == null || credential.getAccessKey() == null || credential.getPrivateKey() == null) {
            return new AltusCredential(null, null);
        } else {
            LOGGER.debug("Fill {} altus credential from {} object...", type, credential.getClass().getName());
            return new AltusCredential(credential.getAccessKey(), credential.getPrivateKey().toCharArray());
        }
    }

    private <C extends CdpCredential> boolean needToGenerateCredential(String accountId, Telemetry telemetry, C credential, String type) {
        if (credential == null || credential.getAccessKey() == null || credential.getPrivateKey() == null) {
            LOGGER.debug("No {} access/secret key is attached to the stack, generate new api key for machine user.", type);
            return true;
        } else if (altusMachineUserService.isCredentialExist(telemetry, accountId, credential.getMachineUserName(), credential.getAccessKey())) {
            LOGGER.debug("{} credential data (machine user: {}; access key: {}) " +
                            "still exists on UMS side, won't generate new ones", type,
                    credential.getMachineUserName(),
                    credential.getAccessKey());
            return false;
        } else {
            LOGGER.debug("{} credential data (machine user: {}; access key: {}) " +
                            "does not exist on UMS side, generate new ones.", type,
                    credential.getMachineUserName(),
                    credential.getAccessKey());
            return true;
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

    private FluentClusterType mapToFluentClusterType(StackType stackType) {
        return StackType.DATALAKE.equals(stackType) ? FluentClusterType.DATALAKE : FluentClusterType.DATAHUB;
    }

    private <T> T convertOrReturnNull(String value, Class<T> type) {
        if (StringUtils.isNotBlank(value)) {
            try {
                return new Json(value).get(type);
            } catch (IOException e) {
                LOGGER.error("Cannot read {} from cluster entity. Continue without value.", type.getSimpleName(), e);
            }
        }
        return null;
    }
}