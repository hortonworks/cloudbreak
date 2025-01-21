package com.sequenceiq.cloudbreak.service.telemetry;

import static java.util.function.Function.identity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.job.dynamicentitlement.DynamicEntitlementRefreshConfig;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.telemetry.monitoring.ExporterConfiguration;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class DynamicEntitlementRefreshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicEntitlementRefreshService.class);

    @Inject
    private DynamicEntitlementRefreshConfig dynamicEntitlementRefreshConfig;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private StackOperations stackOperations;

    @Inject
    private ClusterService clusterService;

    @Inject
    private MonitoringConfiguration monitoringConfiguration;

    public Map<String, Boolean> getChangedWatchedEntitlementsAndStoreNewFromUms(StackDto stack) {
        handleLegacyConfigurations(stack);
        LOGGER.debug("Checking watched entitlement changes for stack {}", stack.getName());
        Map<String, Boolean> umsEntitlements = getEntitlementsFromUms(stack.getResourceCrn(), dynamicEntitlementRefreshConfig.getWatchedEntitlements());
        LOGGER.debug("Watched entitlements from UMS: {}", umsEntitlements);
        Map<String, Boolean> telemetryEntitlements = getOrCreateEntitlementsFromTelemetry(stack.getId(), umsEntitlements);
        LOGGER.debug("Watched entitlements from Telemetry: {}", telemetryEntitlements);
        return getChangedEntitlements(telemetryEntitlements, umsEntitlements);
    }

    private void handleLegacyConfigurations(StackDto stackDto) {
        Optional<String> cmMonitoringUser = getCMMonitoringUser();
        if (stackDto.getCluster().getCloudbreakClusterManagerMonitoringUser() == null && cmMonitoringUser.isPresent()) {
            clusterService.generateClusterManagerMonitoringUserIfMissing(stackDto.getCluster().getId(), cmMonitoringUser.get());
        }
    }

    private Optional<String> getCMMonitoringUser() {
        return Optional.ofNullable(monitoringConfiguration.getClouderaManagerExporter())
                .map(ExporterConfiguration::getUser)
                .filter(StringUtils::isNotBlank);
    }

    public Boolean saltRefreshNeeded(Map<String, Boolean> entitlementsChanged) {
        if (entitlementsChanged == null) {
            return Boolean.FALSE;
        }
        return entitlementsChanged.keySet().stream()
                .anyMatch(dynamicEntitlementRefreshConfig.getCbEntitlements()::contains);
    }

    public Boolean isClusterManagerServerReachable(StackDto stack) {
        LOGGER.debug("Checking that CM is reachable for stack {}", stack.getName());
        return stack.getRunningInstanceMetaDataSet().stream()
                .filter(InstanceMetadataView::getClusterManagerServer)
                .findFirst()
                .map(InstanceMetadataView::isReachable)
                .orElse(Boolean.FALSE);
    }

    public void storeChangedEntitlementsInTelemetry(Long stackId, Map<String, Boolean> changedEntitlements) {
        LOGGER.debug("Storing changed entitlements for stack {}, {}", stackId, changedEntitlements);
        Telemetry telemetry = componentConfigProviderService.getTelemetry(stackId);
        telemetry.getDynamicEntitlements().putAll(changedEntitlements);
        if (changedEntitlements.keySet().contains(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()) &&
                telemetry.getFeatures().getMonitoring() != null) {
            telemetry.getFeatures().getMonitoring().setEnabled(changedEntitlements.get(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
        }
        componentConfigProviderService.replaceTelemetryComponent(stackId, telemetry);
    }

    private Map<String, Boolean> getOrCreateEntitlementsFromTelemetry(Long stackId, Map<String, Boolean> entitlementsFromUms) {
        Telemetry telemetry = componentConfigProviderService.getTelemetry(stackId);
        if (telemetry != null) {
            if (telemetry.getDynamicEntitlements() != null && !telemetry.getDynamicEntitlements().isEmpty()) {
                checkAndStoreMissingWatchedEntitlements(stackId, entitlementsFromUms, telemetry);
                return telemetry.getDynamicEntitlements();
            } else {
                LOGGER.info("Save watched dynamic entitlements to Telemetry, new values: {}", entitlementsFromUms);
                telemetry.setDynamicEntitlements(entitlementsFromUms);
                componentConfigProviderService.replaceTelemetryComponent(stackId, telemetry);
            }
        }
        return entitlementsFromUms;
    }

    private void checkAndStoreMissingWatchedEntitlements(Long stackId, Map<String, Boolean> entitlementsFromUms, Telemetry telemetry) {
        boolean changed = false;
        Map<String, Boolean> newEntitlementsInTelemetry = new HashMap<>();
        for (Entry<String, Boolean> umsEntitlement : entitlementsFromUms.entrySet()) {
            if (!telemetry.getDynamicEntitlements().containsKey(umsEntitlement.getKey())) {
                telemetry.getDynamicEntitlements().put(umsEntitlement.getKey(), umsEntitlement.getValue());
                newEntitlementsInTelemetry.put(umsEntitlement.getKey(), umsEntitlement.getValue());
                changed = true;
            }
        }
        if (changed) {
            LOGGER.info("Save new watched dynamic entitlements to Telemetry, new values: {}", newEntitlementsInTelemetry);
            componentConfigProviderService.replaceTelemetryComponent(stackId, telemetry);
        }
    }

    private Map<String, Boolean> getEntitlementsFromUms(String resourceCrn, Set<String> entitlements) {
        String accountId = Crn.safeFromString(resourceCrn).getAccountId();
        Set<String> foundInUms = entitlementService.getEntitlements(accountId)
                .stream()
                .map(entitlement -> entitlement.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        return entitlements
                .stream()
                .map(entitlement -> entitlement.toUpperCase(Locale.ROOT))
                .collect(Collectors.toMap(identity(), entitlement -> foundInUms.contains(entitlement)));
    }

    private Map<String, Boolean> getChangedEntitlements(Map<String, Boolean> stackEntitlements, Map<String, Boolean> umsEntitlements) {
        Map<String, Boolean> result = new HashMap<>();
        for (Entry<String, Boolean> umsEntitlement : umsEntitlements.entrySet()) {
            Boolean stackValue = stackEntitlements.getOrDefault(umsEntitlement.getKey(), umsEntitlement.getValue());
            if (!umsEntitlement.getValue().equals(stackValue)) {
                result.put(umsEntitlement.getKey(), umsEntitlement.getValue());
            }
        }
        return result;
    }

    public FlowIdentifier changeClusterConfigurationIfEntitlementsChanged(StackDto stack) {
        return stackOperations.refreshEntitlementParams(stack.getResourceCrn());
    }

    public void setupDynamicEntitlementsForProvision(String resourceCrn, Telemetry telemetry) {
        Map<String, Boolean> umsEntitlements = getEntitlementsFromUms(resourceCrn, dynamicEntitlementRefreshConfig.getWatchedEntitlements());
        telemetry.setDynamicEntitlements(umsEntitlements);
    }
}
