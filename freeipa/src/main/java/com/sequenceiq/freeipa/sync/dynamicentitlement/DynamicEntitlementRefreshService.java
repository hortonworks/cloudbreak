package com.sequenceiq.freeipa.sync.dynamicentitlement;

import static java.util.function.Function.identity;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringUrlResolver;
import com.sequenceiq.common.api.telemetry.model.Monitoring;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.stack.dynamicentitlement.RefreshEntitlementParamsFlowChainTriggerEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.telemetry.TelemetryConfigService;

@Service
public class DynamicEntitlementRefreshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicEntitlementRefreshService.class);

    private static final String DIDN_T_CHANGE_MESSAGE = "Watched entitlements didn't change for stack";

    @Inject
    private DynamicEntitlementRefreshConfig dynamicEntitlementRefreshConfig;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private TelemetryConfigService telemetryConfigService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private OperationService operationService;

    @Inject
    private MonitoringUrlResolver monitoringUrlResolver;

    public Map<String, Boolean> getChangedWatchedEntitlementsAndStoreNewFromUms(Stack stack) {
        Map<String, Boolean> umsEntitlements = getEntitlementsFromUms(stack.getResourceCrn(), dynamicEntitlementRefreshConfig.getWatchedEntitlements());
        Map<String, Boolean> telemetryEntitlements = getOrCreateEntitlementsFromTelemetry(stack, umsEntitlements);
        return getChangedEntitlements(telemetryEntitlements, umsEntitlements);
    }

    public Boolean saltRefreshNeeded(Map<String, Boolean> entitlementsChanged) {
        return entitlementsChanged != null && !entitlementsChanged.isEmpty();
    }

    public boolean previousOperationFailed(Stack stack, String operationId) {
        if (operationId != null) {
            try {
                Operation operation = operationService.getOperationForAccountIdAndOperationId(stack.getAccountId(), operationId);
                return OperationState.FAILED == operation.getStatus();
            } catch (NotFoundException e) {
                LOGGER.debug("Operation {} not found.", operationId);
            }
        }
        return false;
    }

    public void storeChangedEntitlementsInTelemetry(Stack stack, Map<String, Boolean> changedEntitlements) {
        LOGGER.debug("Storing changed entitlements for stack {}, {}", stack.getName(), changedEntitlements);
        Telemetry telemetry = stack.getTelemetry();
        telemetry.getDynamicEntitlements().putAll(changedEntitlements);
        if (changedEntitlements.keySet().contains(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()) &&
                telemetry.getFeatures().getMonitoring() != null) {
            telemetry.getFeatures().getMonitoring().setEnabled(changedEntitlements.get(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
            if (changedEntitlements.get(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name())) {
                Monitoring monitoring = new Monitoring();
                monitoring.setRemoteWriteUrl(monitoringUrlResolver.resolve(stack.getAccountId(), entitlementService.isCdpSaasEnabled(stack.getAccountId())));
                telemetry.setMonitoring(monitoring);
            }
        }
        telemetryConfigService.storeTelemetry(stack.getId(), telemetry);
    }

    private Map<String, Boolean> getOrCreateEntitlementsFromTelemetry(Stack stack, Map<String, Boolean> entitlementsFromUms) {
        Telemetry telemetry = stack.getTelemetry();
        if (telemetry != null) {
            if (telemetry.getDynamicEntitlements() != null && !telemetry.getDynamicEntitlements().isEmpty()) {
                checkAndStoreMissingWatchedEntitlements(stack, entitlementsFromUms, telemetry);
                return telemetry.getDynamicEntitlements();
            } else {
                LOGGER.info("Save watched dynamic entitlements to Telemetry, new values: {}", entitlementsFromUms);
                telemetry.setDynamicEntitlements(entitlementsFromUms);
                telemetryConfigService.storeTelemetry(stack.getId(), telemetry);
            }
        }
        return entitlementsFromUms;
    }

    private void checkAndStoreMissingWatchedEntitlements(Stack stack, Map<String, Boolean> entitlementsFromUms, Telemetry telemetry) {
        Map<String, Boolean> newEntitlementsInTelemetry = new HashMap<>();
        for (Entry<String, Boolean> umsEntitlement : entitlementsFromUms.entrySet()) {
            if (!telemetry.getDynamicEntitlements().containsKey(umsEntitlement.getKey())) {
                telemetry.getDynamicEntitlements().put(umsEntitlement.getKey(), umsEntitlement.getValue());
                newEntitlementsInTelemetry.put(umsEntitlement.getKey(), umsEntitlement.getValue());
            }
        }
        if (!newEntitlementsInTelemetry.isEmpty()) {
            LOGGER.info("Save new watched dynamic entitlements to Telemetry, new values: {}", newEntitlementsInTelemetry);
            telemetryConfigService.storeTelemetry(stack.getId(), telemetry);
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

    public String changeClusterConfigurationIfEntitlementsChanged(Stack stack) {
        Map<String, Boolean> changedEntitlements = getChangedWatchedEntitlementsAndStoreNewFromUms(stack);
        if (!changedEntitlements.isEmpty()) {
            LOGGER.info("Start '{}' operation", OperationType.CHANGE_DYNAMIC_ENTITLEMENTS.name());
            Operation operation = operationService.startOperation(stack.getAccountId(), OperationType.CHANGE_DYNAMIC_ENTITLEMENTS,
                    List.of(stack.getEnvironmentCrn()), List.of());
            if (OperationState.RUNNING == operation.getStatus()) {
                LOGGER.info("Changed entitlements for FreeIpa: {}, salt refresh needed: {}", changedEntitlements, true);
                String selector = FlowChainTriggers.REFRESH_ENTITLEMENT_PARAM_CHAIN_TRIGGER_EVENT;
                Acceptable triggerEvent = new RefreshEntitlementParamsFlowChainTriggerEvent(selector, operation.getOperationId(),
                        stack.getId(), changedEntitlements, true);
                flowManager.notify(selector, triggerEvent);
            } else {
                LOGGER.warn("Operation isn't in RUNNING state: {}", operation);
            }
            return operation.getOperationId();
        } else {
            LOGGER.info("Couldn't start refresh entitlement params flow. Watched entitlements didn't change");
        }
        return null;
    }

    public void setupDynamicEntitlementsForProvision(String resourceCrn, Telemetry telemetry) {
        Map<String, Boolean> umsEntitlements = getEntitlementsFromUms(resourceCrn, dynamicEntitlementRefreshConfig.getWatchedEntitlements());
        telemetry.setDynamicEntitlements(umsEntitlements);
    }
}
