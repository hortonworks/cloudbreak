package com.sequenceiq.freeipa.service.telemetry;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

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
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentType;
import com.sequenceiq.cloudbreak.telemetry.VmLogsService;
import com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigService;
import com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigView;
import com.sequenceiq.cloudbreak.telemetry.databus.DatabusConfigService;
import com.sequenceiq.cloudbreak.telemetry.databus.DatabusConfigView;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentConfigService;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentConfigView;
import com.sequenceiq.cloudbreak.telemetry.nodestatus.NodeStatusConfigService;
import com.sequenceiq.cloudbreak.telemetry.nodestatus.NodeStatusConfigView;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryConfigProvider;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.AltusMachineUserService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class TelemetryConfigService implements TelemetryConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryConfigService.class);

    @Value("${info.app.version:}")
    private String version;

    @Inject
    private FluentConfigService fluentConfigService;

    @Inject
    private DatabusConfigService databusConfigService;

    @Inject
    private NodeStatusConfigService nodeStatusConfigService;

    @Inject
    private TelemetryCommonConfigService telemetryCommonConfigService;

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

    @Override
    public Map<String, SaltPillarProperties> createTelemetryConfigs(Long stackId, Set<TelemetryComponentType> components)
            throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getStackById(stackId);
        return createTelemetryPillarConfig(stack);
    }

    public Map<String, SaltPillarProperties> createTelemetryPillarConfig(Stack stack) throws CloudbreakOrchestratorFailedException {
        Telemetry telemetry = stack.getTelemetry();
        if (telemetry != null) {
            String databusEndpoint = getDatabusEndpoint(stack, telemetry);
            boolean databusEnabled = telemetry.isClusterLogsCollectionEnabled();
            boolean databusEndpointValidation = entitlementService.isFreeIpaDatabusEndpointValidationEnabled(stack.getAccountId());
            Map<String, SaltPillarProperties> servicePillarConfig = new HashMap<>();
            servicePillarConfig.putAll(getTelemetryCommonPillarConfig(stack, telemetry, databusEndpoint, databusEndpointValidation));
            servicePillarConfig.putAll(getFluentPillarConfig(stack, telemetry, databusEnabled));
            servicePillarConfig.putAll(getDatabusPillarConfig(stack, databusEndpoint, databusEnabled));
            servicePillarConfig.putAll(getCdpNodeStatusPillarConfig(stack));
            return servicePillarConfig;
        } else {
            return Map.of();
        }
    }

    private Map<String, SaltPillarProperties> getDatabusPillarConfig(Stack stack, String databusEndpoint, boolean databusEnabled)
            throws CloudbreakOrchestratorFailedException {
        if (databusEnabled) {
            LOGGER.debug("Apply DataBus related pillars.");
            try {
                DataBusCredential dbusCredential = altusMachineUserService.getOrCreateDataBusCredentialIfNeeded(stack);
                DatabusConfigView databusConfigView =
                        databusConfigService.createDatabusConfigs(dbusCredential.getAccessKey(), dbusCredential.getPrivateKey().toCharArray(),
                                null, databusEndpoint);
                return Map.of("databus",
                        new SaltPillarProperties("/databus/init.sls", Collections.singletonMap("databus", databusConfigView.toMap())));
            } catch (IOException e) {
                throw new CloudbreakOrchestratorFailedException(e);
            }
        } else {
            return Map.of();
        }
    }

    private Map<String, SaltPillarProperties> getFluentPillarConfig(Stack stack, Telemetry telemetry, boolean databusEnabled) {
        FluentConfigView fluentConfigView = getFluentConfigView(stack, telemetry, databusEnabled);
        return Map.of("fluent", new SaltPillarProperties("/fluent/init.sls", Collections.singletonMap("fluent", fluentConfigView.toMap())));
    }

    private Map<String, SaltPillarProperties> getTelemetryCommonPillarConfig(Stack stack, Telemetry telemetry, String databusEndpoint,
            boolean databusEndpointValidation) {
        TelemetryClusterDetails clusterDetails = TelemetryClusterDetails.Builder.builder()
                .withVersion(version)
                .withPlatform(stack.getCloudPlatform())
                .withCrn(stack.getResourceCrn())
                .withName(stack.getName())
                .withType(FluentClusterType.FREEIPA.value())
                .withOwner(stack.getOwner())
                .withDatabusEndpoint(databusEndpoint)
                .withDatabusS3Endpoint(dataBusEndpointProvider.getDatabusS3Endpoint(databusEndpoint))
                .withDatabusEndpointValidation(databusEndpointValidation)
                .build();
        TelemetryCommonConfigView telemetryCommonConfigs = telemetryCommonConfigService.createTelemetryCommonConfigs(
                telemetry, vmLogsService.getVmLogs(), clusterDetails);
        return Map.of("telemetry",
                new SaltPillarProperties("/telemetry/init.sls", Map.of("telemetry", telemetryCommonConfigs.toMap(),
                        "cloudera-manager", getPaywallConfigs(stack))));
    }

    private String getDatabusEndpoint(Stack stack, Telemetry telemetry) {
        boolean useDbusCnameEndpoint = entitlementService.useDataBusCNameEndpointEnabled(stack.getAccountId());
        return dataBusEndpointProvider.getDataBusEndpoint(telemetry.getDatabusEndpoint(), useDbusCnameEndpoint);
    }

    private FluentConfigView getFluentConfigView(Stack stack, Telemetry telemetry, boolean databusEnabled) {
        TelemetryClusterDetails clusterDetails = TelemetryClusterDetails.Builder.builder()
                .withOwner(stack.getOwner())
                .withName(stack.getName())
                .withType(FluentClusterType.FREEIPA.value())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVersion(version)
                .build();
        return fluentConfigService.createFluentConfigs(clusterDetails, databusEnabled, false, stack.getRegion(), telemetry);
    }

    private Map<String, ? extends SaltPillarProperties> getCdpNodeStatusPillarConfig(Stack stack) {
        char[] passwordInput = null;
        if (StringUtils.isNotBlank(stack.getCdpNodeStatusMonitorPassword())) {
            passwordInput = stack.getCdpNodeStatusMonitorPassword().toCharArray();
        }
        boolean saltPingEnabled = entitlementService.nodestatusSaltPingEnabled(stack.getAccountId());
        NodeStatusConfigView nodeStatusConfigs = nodeStatusConfigService.createNodeStatusConfig(
                stack.getCdpNodeStatusMonitorUser(), passwordInput, saltPingEnabled);
        return Map.of("nodestatus",
                new SaltPillarProperties("/nodestatus/init.sls", Collections.singletonMap("nodestatus", nodeStatusConfigs.toMap())));
    }

    private Map<String, Object> getPaywallConfigs(Stack stack) {
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        UserManagementProto.Account account = umsClient.getAccountDetails(accountId, MDCUtils.getRequestId());
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
