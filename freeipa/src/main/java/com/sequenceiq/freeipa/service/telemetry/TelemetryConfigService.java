package com.sequenceiq.freeipa.service.telemetry;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.CMLicenseParser;
import com.sequenceiq.cloudbreak.auth.JsonCMLicense;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentType;
import com.sequenceiq.cloudbreak.telemetry.TelemetryContextProvider;
import com.sequenceiq.cloudbreak.telemetry.context.MeteringContext;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringServiceType;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetrySaltPillarDecorator;
import com.sequenceiq.cloudbreak.telemetry.VmLogsService;
import com.sequenceiq.cloudbreak.telemetry.context.DatabusContext;
import com.sequenceiq.cloudbreak.telemetry.context.LogShipperContext;
import com.sequenceiq.cloudbreak.telemetry.context.MonitoringContext;
import com.sequenceiq.cloudbreak.telemetry.context.NodeStatusContext;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringClusterType;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfigService;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryConfigProvider;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.MonitoringCredential;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.VmLog;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.AltusMachineUserService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class TelemetryConfigService implements TelemetryConfigProvider, TelemetryContextProvider<Stack> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryConfigService.class);

    @Value("${info.app.version:}")
    private String version;

    @Inject
    private MonitoringConfigService monitoringConfigService;

    @Inject
    private VmLogsService vmLogsService;

    @Inject
    private AltusMachineUserService altusMachineUserService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private DataBusEndpointProvider dataBusEndpointProvider;

    @Inject
    private CMLicenseParser cmLicenseParser;

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    private StackService stackService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private TelemetrySaltPillarDecorator telemetrySaltPillarDecorator;

    @Override
    public Map<String, SaltPillarProperties> createTelemetryConfigs(Long stackId, Set<TelemetryComponentType> components) {
        Stack stack = stackService.getStackById(stackId);
        return createTelemetryPillarConfig(stack);
    }

    public Map<String, SaltPillarProperties> createTelemetryPillarConfig(Stack stack) {
        return telemetrySaltPillarDecorator.generatePillarConfigMap(stack);
    }

    @Override
    public TelemetryContext createTelemetryContext(Stack stack) {
        TelemetryContext telemetryContext = new TelemetryContext();
        Telemetry telemetry = stack.getTelemetry();
        telemetryContext.setTelemetry(telemetry);
        telemetryContext.setClusterType(FluentClusterType.FREEIPA);
        DatabusContext databusContext = createDatabusContext(stack, telemetry);
        telemetryContext.setDatabusContext(databusContext);
        telemetryContext.setClusterDetails(createClusterDetails(stack, databusContext));
        NodeStatusContext nodeStatusContext = createNodeStatusContext(stack);
        telemetryContext.setNodeStatusContext(nodeStatusContext);
        if (telemetry != null) {
            telemetryContext.setLogShipperContext(createLogShipperContext(stack, telemetry));
            telemetryContext.setMonitoringContext(createMonitoringContext(stack, telemetry, nodeStatusContext));
            telemetryContext.setPaywallConfigs(getPaywallConfigs(stack));
            telemetryContext.setMeteringContext(MeteringContext.builder().build());
        }
        return telemetryContext;
    }

    private NodeStatusContext createNodeStatusContext(Stack stack) {
        NodeStatusContext.Builder builder = NodeStatusContext.builder();
        boolean saltPingEnabled = entitlementService.nodestatusSaltPingEnabled(stack.getAccountId());
        if (saltPingEnabled) {
            builder.saltPingEnabled();
        }
        if (StringUtils.isNotBlank(stack.getCdpNodeStatusMonitorPassword())) {
            builder.withPassword(stack.getCdpNodeStatusMonitorPassword().toCharArray());
        }
        return builder
                .withUsername(stack.getCdpNodeStatusMonitorUser())
                .build();
    }

    private MonitoringContext createMonitoringContext(Stack stack, Telemetry telemetry, NodeStatusContext nodeStatusContext) {
        boolean cdpSaasEnabled = entitlementService.isCdpSaasEnabled(stack.getAccountId());
        boolean computeMonitoring = entitlementService.isComputeMonitoringEnabled(stack.getAccountId());
        boolean monitoringEnabled = monitoringConfigService.isMonitoringEnabled(cdpSaasEnabled, computeMonitoring);
        MonitoringContext.Builder builder = MonitoringContext.builder();
        if (monitoringEnabled) {
            builder.enabled();
            try {
                Optional<MonitoringCredential> monitoringCredential = altusMachineUserService.getOrCreateMonitoringCredentialIfNeeded(stack);
                builder.withCredential(monitoringCredential.orElse(null));
            } catch (IOException e) {
                throw new CloudbreakServiceException(e);
            }
        }
        if (cdpSaasEnabled) {
            builder.withServiceType(MonitoringServiceType.SAAS);
        }
        builder.withClusterType(MonitoringClusterType.FREEIPA);
        builder.withSharedPassword(nodeStatusContext.getPassword());
        if (telemetry.getMonitoring() != null) {
            builder.withRemoteWriteUrl(telemetry.getMonitoring().getRemoteWriteUrl());
        }
        return builder.build();
    }

    private LogShipperContext createLogShipperContext(Stack stack, Telemetry telemetry) {
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

    private DatabusContext createDatabusContext(Stack stack, Telemetry telemetry) {
        DatabusContext.Builder builder = DatabusContext.builder();
        if (telemetry == null) {
            return builder.build();
        }
        if (telemetry.isAnyDataBusBasedFeatureEnablred()) {
            LOGGER.debug("Apply DataBus related configurations.");
            builder.enabled();
            if (entitlementService.isFreeIpaDatabusEndpointValidationEnabled(stack.getAccountId())) {
                builder.withValidation();
            }
            try {
                DataBusCredential credential = altusMachineUserService.getOrCreateDataBusCredentialIfNeeded(stack);
                builder.withCredential(credential);
            } catch (IOException e) {
                throw new CloudbreakServiceException(e);
            }
        }
        String databusEndpoint = getDatabusEndpoint(stack, telemetry);
        builder.withEndpoint(databusEndpoint)
                .withS3Endpoint(dataBusEndpointProvider.getDatabusS3Endpoint(databusEndpoint));
        return builder.build();
    }

    private TelemetryClusterDetails createClusterDetails(Stack stack, DatabusContext databusContext) {
        return TelemetryClusterDetails.Builder.builder()
                .withVersion(version)
                .withPlatform(stack.getCloudPlatform())
                .withCrn(stack.getResourceCrn())
                .withName(stack.getName())
                .withType(FluentClusterType.FREEIPA.value())
                .withOwner(stack.getOwner())
                .withDatabusEndpoint(databusContext.getEndpoint())
                .withDatabusS3Endpoint(databusContext.getS3Endpoint())
                .withDatabusEndpointValidation(databusContext.isValidation())
                .build();
    }

    private String getDatabusEndpoint(Stack stack, Telemetry telemetry) {
        boolean useDbusCnameEndpoint = entitlementService.useDataBusCNameEndpointEnabled(stack.getAccountId());
        return dataBusEndpointProvider.getDataBusEndpoint(telemetry.getDatabusEndpoint(), useDbusCnameEndpoint);
    }

    private Map<String, Object> getPaywallConfigs(Stack stack) {
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        UserManagementProto.Account account = umsClient.getAccountDetails(accountId, regionAwareInternalCrnGeneratorFactory);
        Optional<JsonCMLicense> license = Optional.of(account.getClouderaManagerLicenseKey())
                .flatMap(cmLicenseParser::parseLicense);
        if (license.isPresent()) {
            return createConfigWhenCmLicenseAvailable(license.get());
        } else {
            LOGGER.debug("No CM license available");
            return Map.of();
        }
    }

    private Map<String, Object> createConfigWhenCmLicenseAvailable(JsonCMLicense license) {
        String username = license.getPaywallUsername();
        String password = license.getPaywallPassword();
        if (isNotEmpty(username) && isNotEmpty(password)) {
            LOGGER.debug("Setting paywall license in pillar");
            return Map.of("paywall_username", username, "paywall_password", password);
        } else {
            LOGGER.debug("While CM license exist the username or password is empty");
            return Map.of();
        }
    }
}
