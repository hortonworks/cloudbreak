package com.sequenceiq.freeipa.service.telemetry;

import static com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider.CipherSuitesLimitType.BLACKBOX_EXPORTER;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
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
import com.sequenceiq.cloudbreak.auth.altus.model.CdpAccessKeyType;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentType;
import com.sequenceiq.cloudbreak.telemetry.TelemetryContextProvider;
import com.sequenceiq.cloudbreak.telemetry.TelemetryFeatureService;
import com.sequenceiq.cloudbreak.telemetry.VmLogsService;
import com.sequenceiq.cloudbreak.telemetry.context.DatabusContext;
import com.sequenceiq.cloudbreak.telemetry.context.LogShipperContext;
import com.sequenceiq.cloudbreak.telemetry.context.MonitoringContext;
import com.sequenceiq.cloudbreak.telemetry.context.NodeStatusContext;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringClusterType;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringServiceType;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringUrlResolver;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryConfigProvider;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetrySaltPillarDecorator;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Monitoring;
import com.sequenceiq.common.api.telemetry.model.MonitoringCredential;
import com.sequenceiq.common.api.telemetry.model.SensitiveLoggingComponent;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.VmLog;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.AltusMachineUserService;
import com.sequenceiq.freeipa.service.client.CachedEncryptionProfileClientService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class TelemetryConfigService implements TelemetryConfigProvider, TelemetryContextProvider<Stack> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryConfigService.class);

    @Value("${info.app.version:}")
    private String version;

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
    private TelemetrySaltPillarDecorator telemetrySaltPillarDecorator;

    @Inject
    private ImageService imageService;

    @Inject
    private TelemetryFeatureService telemetryFeatureService;

    @Inject
    private MonitoringUrlResolver monitoringUrlResolver;

    @Inject
    private TransactionService transactionService;

    @Inject
    private CachedEnvironmentClientService environmentService;

    @Inject
    private EncryptionProfileProvider encryptionProfileProvider;

    @Inject
    private CachedEncryptionProfileClientService cachedEncryptionProfileClientService;

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
        updateTelemetryIfMonitoringIsEnabled(stack, telemetry);
        telemetryContext.setTelemetry(telemetry);
        telemetryContext.setClusterType(FluentClusterType.FREEIPA);
        CdpAccessKeyType cdpAccessKeyType = getCdpAccessKeyType(stack);
        DatabusContext databusContext = createDatabusContext(stack, telemetry, cdpAccessKeyType);
        telemetryContext.setDatabusContext(databusContext);
        telemetryContext.setClusterDetails(createClusterDetails(stack, databusContext));
        NodeStatusContext nodeStatusContext = createNodeStatusContext(stack);
        telemetryContext.setNodeStatusContext(nodeStatusContext);
        DetailedEnvironmentResponse environmentResponse = environmentService.getByCrn(stack.getEnvironmentCrn());

        EncryptionProfileResponse encryptionProfileResponse = cachedEncryptionProfileClientService.getByNameOrDefaultIfEmpty(
                environmentResponse.getEncryptionProfileName());
        Map<String, List<String>> userCipherSuits =
                Optional.ofNullable(encryptionProfileResponse)
                        .map(EncryptionProfileResponse::getCipherSuites)
                        .orElse(null);
        List<String> tlsCipherSuitesBlackBoxExporter = encryptionProfileProvider
                .getTlsCipherSuitesIanaList(userCipherSuits, BLACKBOX_EXPORTER);
        telemetryContext.setTlsCipherSuites(tlsCipherSuitesBlackBoxExporter);
        if (telemetry != null) {
            telemetryContext.setLogShipperContext(createLogShipperContext(stack, telemetry));
            telemetryContext.setMonitoringContext(createMonitoringContext(stack, telemetry, nodeStatusContext, cdpAccessKeyType));
            telemetryContext.setPaywallConfigs(getPaywallConfigs(stack));
        }
        telemetryContext.setOsType(stack.getImage().getOsType());
        return telemetryContext;
    }

    private void updateTelemetryIfMonitoringIsEnabled(Stack stack, Telemetry telemetry) {
        if (telemetry != null) {
            boolean computeMonitoringEntitled = entitlementService.isComputeMonitoringEnabled(stack.getAccountId());
            if (!telemetry.isComputeMonitoringEnabled() && computeMonitoringEntitled) {
                Monitoring monitoring = new Monitoring();
                monitoring.setRemoteWriteUrl(monitoringUrlResolver.resolve(stack.getAccountId(), entitlementService.isCdpSaasEnabled(stack.getAccountId())));
                telemetry.setMonitoring(monitoring);
                storeTelemetry(stack.getId(), telemetry);
            } else if (telemetry.isComputeMonitoringEnabled() && !computeMonitoringEntitled) {
                telemetry.setMonitoring(new Monitoring());
                storeTelemetry(stack.getId(), telemetry);
            }
        }
    }

    public void storeTelemetry(Long stackId, Telemetry telemetry) {
        try {
            transactionService.required(() -> {
                Stack stack = stackService.getStackById(stackId);
                stack.setTelemetry(telemetry);
                stackService.save(stack);
            });
        } catch (TransactionService.TransactionExecutionException e) {
            throw new TransactionService.TransactionRuntimeExecutionException(e);
        }
    }

    private NodeStatusContext createNodeStatusContext(Stack stack) {
        NodeStatusContext.Builder builder = NodeStatusContext.builder();
        if (StringUtils.isNotBlank(stack.getCdpNodeStatusMonitorPassword())) {
            builder.withPassword(stack.getCdpNodeStatusMonitorPassword());
        }
        return builder.build();
    }

    private MonitoringContext createMonitoringContext(Stack stack, Telemetry telemetry, NodeStatusContext nodeStatusContext,
            CdpAccessKeyType cdpAccessKeyType) {
        MonitoringContext.Builder builder = MonitoringContext.builder();
        if (entitlementService.isComputeMonitoringEnabled(stack.getAccountId())) {
            builder.enabled();
            try {
                Optional<MonitoringCredential> monitoringCredential = altusMachineUserService.getOrCreateMonitoringCredentialIfNeeded(stack, cdpAccessKeyType);
                builder.withCredential(monitoringCredential.orElse(null));
            } catch (IOException e) {
                throw new CloudbreakServiceException(e);
            }
            if (telemetry.isComputeMonitoringEnabled()) {
                builder.withRemoteWriteUrl(telemetry.getMonitoring().getRemoteWriteUrl());
            } else {
                builder.withRemoteWriteUrl(monitoringUrlResolver.resolve(stack.getAccountId(), entitlementService.isCdpSaasEnabled(stack.getAccountId())));
            }
        }
        if (entitlementService.isCdpSaasEnabled(stack.getAccountId())) {
            builder.withServiceType(MonitoringServiceType.SAAS);
        }
        builder.withClusterType(MonitoringClusterType.FREEIPA);
        builder.withSharedPassword(nodeStatusContext.getPassword());
        return builder.build();
    }

    private LogShipperContext createLogShipperContext(Stack stack, Telemetry telemetry) {
        LogShipperContext.Builder builder = LogShipperContext.builder();
        List<VmLog> vmLogList = vmLogsService.getVmLogs();
        Logging logging = telemetry.getLogging();
        if (telemetry.isCloudStorageLoggingEnabled() && logging != null
                && ObjectUtils.anyNotNull(logging.getS3(), logging.getAdlsGen2(), logging.getGcs())) {
            builder.enabled().cloudStorageLogging();
            if (CollectionUtils.emptyIfNull(logging.getEnabledSensitiveStorageLogs()).contains(SensitiveLoggingComponent.SALT)) {
                builder.includeSaltLogsInCloudStorageLogs();
            }
            if (isPreferMinifiLogging(stack)) {
                builder.preferMinifiLogging();
            }
        }
        return builder
                .withVmLogs(vmLogList)
                .withCloudRegion(stack.getRegion())
                .build();
    }

    private DatabusContext createDatabusContext(Stack stack, Telemetry telemetry, CdpAccessKeyType cdpAccessKeyType) {
        DatabusContext.Builder builder = DatabusContext.builder();
        if (telemetry == null) {
            return builder.build();
        }
        LOGGER.debug("Apply DataBus related configurations.");
        builder.enabled();
        if (entitlementService.isFreeIpaDatabusEndpointValidationEnabled(stack.getAccountId())) {
            builder.withValidation();
        }
        try {
            DataBusCredential credential = altusMachineUserService.getOrCreateDataBusCredentialIfNeeded(stack, cdpAccessKeyType);
            builder.withCredential(credential);
        } catch (IOException e) {
            throw new CloudbreakServiceException(e);
        }
        String databusEndpoint = getDatabusEndpoint(stack, telemetry);
        builder.withEndpoint(databusEndpoint)
                .withS3Endpoint(dataBusEndpointProvider.getDatabusS3Endpoint(databusEndpoint, stack.getRegion()));
        return builder.build();
    }

    private TelemetryClusterDetails createClusterDetails(Stack stack, DatabusContext databusContext) {
        return TelemetryClusterDetails.Builder.builder()
                .withVersion(version)
                .withPlatform(stack.getCloudPlatform())
                .withCrn(stack.getResourceCrn())
                .withEnvironmentCrn(stack.getEnvironmentCrn())
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
        UserManagementProto.Account account = umsClient.getAccountDetails(accountId);
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

    public CdpAccessKeyType getCdpAccessKeyType(Stack stack) {
        if (AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value().equals(stack.getPlatformvariant())) {
            Image image = imageService.getImageForStack(stack);
            if (telemetryFeatureService.isECDSAAccessKeyTypeSupported(image.getPackageVersions())) {
                return CdpAccessKeyType.ECDSA;
            } else {
                throw new CloudbreakRuntimeException("The image contains packages which can't support ECDSA key, but ECDSA is mandatory on Gov environment");
            }
        } else {
            return CdpAccessKeyType.ED25519;
        }
    }

    private boolean isPreferMinifiLogging(Stack stack) {
        if (entitlementService.isPreferMinifiLogging(stack.getAccountId())) {
            Image image = imageService.getImageForStack(stack);
            return telemetryFeatureService.isMinifiLoggingSupported(image.getPackageVersions());
        }
        return false;
    }
}
