package com.sequenceiq.freeipa.sync.dynamicentitlement;

import static java.util.function.Function.identity;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringUrlResolver;
import com.sequenceiq.common.api.telemetry.model.Monitoring;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.service.FlowService;
import com.sequenceiq.freeipa.entity.DynamicEntitlement;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.stack.dynamicentitlement.RefreshEntitlementParamsFlowChainTriggerEvent;
import com.sequenceiq.freeipa.service.DynamicEntitlementService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class DynamicEntitlementRefreshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicEntitlementRefreshService.class);

    private static final String DIDN_T_CHANGE_MESSAGE = "Watched entitlements didn't change for stack";

    @Inject
    private DynamicEntitlementRefreshConfig dynamicEntitlementRefreshConfig;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private FlowService flowService;

    @Inject
    private MonitoringUrlResolver monitoringUrlResolver;

    @Inject
    private TransactionService transactionService;

    @Inject
    private StackService stackService;

    @Inject
    private DynamicEntitlementService dynamicEntitlementService;

    public Map<String, Boolean> getChangedWatchedEntitlementsAndStoreNewFromUms(Stack stack) {
        Map<String, Boolean> umsEntitlements = getEntitlementsFromUms(stack.getResourceCrn(), dynamicEntitlementRefreshConfig.getWatchedEntitlements());
        Map<String, Boolean> telemetryEntitlements = getOrCreateEntitlementsFromTelemetry(stack, umsEntitlements);
        return getChangedEntitlements(telemetryEntitlements, umsEntitlements);
    }

    public Boolean saltRefreshNeeded(Map<String, Boolean> entitlementsChanged) {
        return entitlementsChanged != null && !entitlementsChanged.isEmpty();
    }

    public boolean previousFlowFailed(Stack stack, String flowChainId) {
        return flowService.isPreviousFlowFailed(stack.getId(), flowChainId);
    }

    public void storeChangedEntitlementsAndTelemetry(Stack stack, Map<String, Boolean> changedEntitlements) {
        LOGGER.debug("Storing changed entitlements for stack {}, {}", stack.getName(), changedEntitlements);
        Telemetry telemetry = stack.getTelemetry();
        Set<DynamicEntitlement> dynamicEntitlements = new HashSet<>();
        Set<DynamicEntitlement> storedDynamicEntitlements = dynamicEntitlementService.findByStackId(stack.getId());
        if (storedDynamicEntitlements != null && !storedDynamicEntitlements.isEmpty()) {
            dynamicEntitlements = mergeDynamicEntitlementSets(stack, storedDynamicEntitlements, changedEntitlements);
        } else {
            dynamicEntitlements = convertMapToDynamicEntitlementSet(stack, changedEntitlements);
        }
        if (changedEntitlements.keySet().contains(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()) &&
                telemetry.getFeatures().getMonitoring() != null) {
            telemetry.getFeatures().getMonitoring().setEnabled(changedEntitlements.get(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
            if (changedEntitlements.get(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name())) {
                Monitoring monitoring = new Monitoring();
                monitoring.setRemoteWriteUrl(monitoringUrlResolver.resolve(stack.getAccountId(), entitlementService.isCdpSaasEnabled(stack.getAccountId())));
                telemetry.setMonitoring(monitoring);
            }
        }
        saveDynamicEntitlementAndTelemetry(stack.getId(), telemetry, dynamicEntitlements);
    }

    private Set<DynamicEntitlement> mergeDynamicEntitlementSets(Stack stack,
            Set<DynamicEntitlement> storedEntitlements, Map<String, Boolean> changedEntitlements) {
        Set<DynamicEntitlement> changedSet = convertMapToDynamicEntitlementSet(stack, changedEntitlements);
        storedEntitlements.removeAll(changedSet);
        storedEntitlements.addAll(changedSet);
        return storedEntitlements;
    }

    private Set<DynamicEntitlement> convertMapToDynamicEntitlementSet(Stack stack, Map<String, Boolean> entitlements) {
        Set<DynamicEntitlement> result = new HashSet<>();
        if (entitlements != null) {
            for (Map.Entry<String, Boolean> entitlement : entitlements.entrySet()) {
                result.add(new DynamicEntitlement(entitlement.getKey(), entitlement.getValue(), stack));
            }
        }
        return result;
    }

    private Map<String, Boolean> convertDynamicEntitlementSetToMap(Set<DynamicEntitlement> dynamicEntitlements) {
        Map<String, Boolean> result = new HashMap<>();
        if (dynamicEntitlements != null) {
            for (DynamicEntitlement dynamicEntitlement : dynamicEntitlements) {
                result.put(dynamicEntitlement.getEntitlement(), dynamicEntitlement.getEntitlementValue());
            }
        }
        return result;
    }

    private void saveDynamicEntitlementAndTelemetry(Long stackId, Telemetry telemetry, Set<DynamicEntitlement> dynamicEntitlements) {
        try {
            transactionService.required(() -> {
                Stack stack = stackService.getStackById(stackId);
                if (dynamicEntitlements != null) {
                    dynamicEntitlementService.saveNew(stackId, dynamicEntitlements);
                }
                if (telemetry != null) {
                    stack.setTelemetry(telemetry);
                    stackService.save(stack);
                }
            });
        } catch (TransactionService.TransactionExecutionException e) {
            throw new TransactionService.TransactionRuntimeExecutionException(e);
        }
    }

    private Map<String, Boolean> getOrCreateEntitlementsFromTelemetry(Stack stack, Map<String, Boolean> entitlementsFromUms) {
        boolean storeToNewLocation = false;
        Set<DynamicEntitlement> stackDynamicEntitlements = dynamicEntitlementService.findByStackId(stack.getId());
        Telemetry telemetry = stack.getTelemetry();
        if ((stackDynamicEntitlements == null || stackDynamicEntitlements.isEmpty())  && telemetry != null && telemetry.getDynamicEntitlements() != null) {
            stackDynamicEntitlements = convertMapToDynamicEntitlementSet(stack, telemetry.getDynamicEntitlements());
            telemetry.setDynamicEntitlements(Collections.emptyMap());
            storeToNewLocation = true;
        }
        if (!stackDynamicEntitlements.isEmpty()) {
            return checkAndStoreMissingWatchedEntitlements(stack, entitlementsFromUms, telemetry,
                    stackDynamicEntitlements, storeToNewLocation);
        } else {
            LOGGER.info("Save watched dynamic entitlements, new values: {}", entitlementsFromUms);
            stackDynamicEntitlements = convertMapToDynamicEntitlementSet(stack, entitlementsFromUms);
            saveDynamicEntitlementAndTelemetry(stack.getId(), telemetry, stackDynamicEntitlements);
        }
        return entitlementsFromUms;
    }

    private Map<String, Boolean> checkAndStoreMissingWatchedEntitlements(Stack stack, Map<String, Boolean> entitlementsFromUms, Telemetry telemetry,
            Set<DynamicEntitlement> dynamicEntitlements, boolean storeToNewLocation) {
        Map<String, Boolean> newEntitlements = new HashMap<>();
        for (Entry<String, Boolean> umsEntitlement : entitlementsFromUms.entrySet()) {
            if (!dynamicEntitlements.contains(new DynamicEntitlement(umsEntitlement.getKey(), null, null))) {
                dynamicEntitlements.add(new DynamicEntitlement(umsEntitlement.getKey(), umsEntitlement.getValue(), stack));
                newEntitlements.put(umsEntitlement.getKey(), umsEntitlement.getValue());
            }
        }
        if (!newEntitlements.isEmpty() || storeToNewLocation) {
            LOGGER.info("Save new watched dynamic entitlements, new values: {}", newEntitlements);
            saveDynamicEntitlementAndTelemetry(stack.getId(), telemetry, dynamicEntitlements);
        }
        return convertDynamicEntitlementSetToMap(dynamicEntitlements);
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

    public FlowIdentifier changeClusterConfigurationIfEntitlementsChanged(Stack stack) {
        Map<String, Boolean> changedEntitlements = getChangedWatchedEntitlementsAndStoreNewFromUms(stack);
        if (!changedEntitlements.isEmpty()) {
            LOGGER.info("Changed entitlements for FreeIpa: {}, salt refresh needed: {}", changedEntitlements, true);
            String selector = FlowChainTriggers.REFRESH_ENTITLEMENT_PARAM_CHAIN_TRIGGER_EVENT;
            Acceptable triggerEvent = new RefreshEntitlementParamsFlowChainTriggerEvent(selector, null,
                    stack.getId(), stack.getEnvironmentCrn(), changedEntitlements, isSaltRefreshNeeded(changedEntitlements));
            return flowManager.notify(selector, triggerEvent);
        } else {
            LOGGER.info("Couldn't start refresh entitlement params flow. Watched entitlements didn't change");
        }
        return null;
    }

    private boolean isSaltRefreshNeeded(Map<String, Boolean> changedEntitlements) {
        return changedEntitlements.containsKey(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING);
    }

    public void setupDynamicEntitlementsForProvision(String resourceCrn, Telemetry telemetry) {
        Map<String, Boolean> umsEntitlements = getEntitlementsFromUms(resourceCrn, dynamicEntitlementRefreshConfig.getWatchedEntitlements());
        telemetry.setDynamicEntitlements(umsEntitlements);
    }
}
