package com.sequenceiq.freeipa.flow.chain;


import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.ENTITLEMENT_SYNC_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.ENTITLEMENT_SYNC_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.ENTITLEMENT_SYNC_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.SecretRotationTriggerEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeState;
import com.sequenceiq.flow.core.chain.finalize.flowevents.FlowChainFinalizePayload;
import com.sequenceiq.flow.core.chain.init.config.FlowChainInitState;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.dynamicentitlement.RefreshEntitlementParamsFlowChainTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.dynamicentitlement.RefreshEntitlementParamsTriggerEvent;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class RefreshEntitlementParamsFlowEventChainFactory implements FlowEventChainFactory<RefreshEntitlementParamsFlowChainTriggerEvent>, FreeIpaUseCaseAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshEntitlementParamsFlowEventChainFactory.class);

    @Inject
    private StackService stackService;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.REFRESH_ENTITLEMENT_PARAM_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(RefreshEntitlementParamsFlowChainTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new FlowChainInitPayload(getName(), event.getResourceId(), event.accepted()));
        if (event.getSaltRefreshNeeded()) {
            flowEventChain.add(new SaltUpdateTriggerEvent(event.getResourceId(),
                    event.accepted(), true, false, event.getOperationId()));
            LOGGER.debug("Salt refresh flow is added to RefreshEntitlementParamsFlow.");
        }

        Crn resourceCrn = Crn.fromString(event.getResourceCrn());
        if (Optional.ofNullable(event.getChangedEntitlements().get(Entitlement.CDP_JUMPGATE_ROOT_CA_AUTO_ROTATION.name())).orElse(Boolean.FALSE) &&
                entitlementService.isJumpgateRootCertAutoRotationEnabled(resourceCrn.getAccountId()) &&
                entitlementService.isJumpgateNewRootCertEnabled(resourceCrn.getAccountId())) {
            flowEventChain.add(getSecretRotationTriggerEvent(event.getResourceId(), event.getResourceCrn(), event));
            LOGGER.debug("Jumpgate secret rotation flow is added to RefreshEntitlementParamsFlow.");
        }
        flowEventChain.add(RefreshEntitlementParamsTriggerEvent.fromChainTrigger(event, true, true));
        flowEventChain.add(new FlowChainFinalizePayload(getName(), event.getResourceId(), event.accepted()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    private SecretRotationTriggerEvent getSecretRotationTriggerEvent(Long freeIpaId, String environmentCrn, Acceptable event) {
        return new SecretRotationTriggerEvent(EventSelectorUtil.selector(SecretRotationTriggerEvent.class),
                freeIpaId,
                environmentCrn,
                FreeIpaSecretType.CCMV2_JUMPGATE_AGENT_ACCESS_KEY,
                null,
                null,
                event.accepted());

    }

    public Value getUseCaseForFlowState(Enum flowState) {
        if (FlowChainInitState.INIT_STATE.equals(flowState)) {
            return ENTITLEMENT_SYNC_STARTED;
        } else if (FlowChainFinalizeState.FLOWCHAIN_FINALIZE_FINISHED_STATE.equals(flowState)) {
            return ENTITLEMENT_SYNC_FINISHED;
        } else if (flowState.toString().endsWith("FAILED_STATE")) {
            return ENTITLEMENT_SYNC_FAILED;
        } else {
            return UNSET;
        }
    }
}
