package com.sequenceiq.freeipa.service.telemetry;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
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
import com.sequenceiq.cloudbreak.telemetry.nodestatus.NodeStatusConfigService;
import com.sequenceiq.cloudbreak.telemetry.nodestatus.NodeStatusConfigView;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.AltusMachineUserService;

@Service
public class TelemetryConfigService {

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

    public Map<String, SaltPillarProperties> createTelemetryPillarConfig(Stack stack) throws CloudbreakOrchestratorFailedException {
        Telemetry telemetry = stack.getTelemetry();
        if (telemetry != null) {
            String databusEndpoint = getDatabusEndpoint(stack, telemetry);
            boolean databusEnabled = telemetry.isClusterLogsCollectionEnabled();

            Map<String, SaltPillarProperties> servicePillarConfig = new HashMap<>();
            servicePillarConfig.putAll(getTelemetryCommonPillarConfig(stack, telemetry, databusEndpoint));
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

    private Map<String, SaltPillarProperties> getTelemetryCommonPillarConfig(Stack stack, Telemetry telemetry, String databusEndpoint) {
        TelemetryCommonConfigView telemetryCommonConfigs = telemetryCommonConfigService.createTelemetryCommonConfigs(
                telemetry, vmLogsService.getVmLogs(), FluentClusterType.FREEIPA.value(), stack.getResourceCrn(),
                stack.getName(), stack.getOwner(), stack.getCloudPlatform(), databusEndpoint);
        return Map.of("telemetry",
                new SaltPillarProperties("/telemetry/init.sls", Collections.singletonMap("telemetry", telemetryCommonConfigs.toMap())));
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
        return fluentConfigService.createFluentConfigs(clusterDetails, databusEnabled, false, telemetry);
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
}
