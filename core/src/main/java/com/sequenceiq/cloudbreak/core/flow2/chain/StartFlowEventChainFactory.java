package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.RESUME_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.RESUME_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.RESUME_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartEvent.EXTERNAL_DATABASE_COMMENCE_START_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.STACK_START_EVENT;

import java.util.Arrays;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResource;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.SecretRotationTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.MultiClusterRotationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.event.EventSelectorUtil;

@Component
public class StartFlowEventChainFactory implements FlowEventChainFactory<StackEvent>, ClusterUseCaseAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartFlowEventChainFactory.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private MultiClusterRotationService multiClusterRotationService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.FULL_START_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(StackEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new StackEvent(EXTERNAL_DATABASE_COMMENCE_START_EVENT.event(), event.getResourceId(), event.accepted()));
        flowEventChain.add(new StackEvent(STACK_START_EVENT.event(), event.getResourceId()));
        flowEventChain.add(new StackEvent(CLUSTER_START_EVENT.event(), event.getResourceId()));
        addChildMultiClusterSecretRotationFlowIfNeeded(event, flowEventChain);
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    private void addChildMultiClusterSecretRotationFlowIfNeeded(StackEvent event, Queue<Selectable> flowEventChain) {
        try {
            StackDto stackDto = stackDtoService.getById(event.getResourceId());
            if (StackType.WORKLOAD.equals(stackDto.getType())) {
                String crn = stackDto.getResourceCrn();
                Set<MultiClusterRotationResource> multiRotationEntriesForResource = multiClusterRotationService.getMultiRotationChildEntriesForResource(crn);
                multiRotationEntriesForResource.stream()
                        .map(MultiClusterRotationResource::getSecretType)
                        .forEach(multiSecretType -> Arrays.stream(CloudbreakSecretType.values())
                                .filter(cst -> cst.getMultiSecretType().stream().anyMatch(multiSecretType::equals))
                                .findFirst()
                                .ifPresent(cst -> flowEventChain.add(getSecretRotationTriggerEvent(event, cst, crn))));
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to decide if multi-cluster rotation is needed for the cluster, thus skipping it.", e);
        }
    }

    private static SecretRotationTriggerEvent getSecretRotationTriggerEvent(StackEvent event, CloudbreakSecretType cst, String crn) {
        return new SecretRotationTriggerEvent(EventSelectorUtil.selector(SecretRotationTriggerEvent.class),
                event.getResourceId(), crn, cst, null, null, event.accepted());
    }

    @Override
    public Value getUseCaseForFlowState(Enum flowState) {
        if (StackStartState.INIT_STATE.equals(flowState)) {
            return RESUME_STARTED;
        } else if (ClusterStartState.CLUSTER_START_FINISHED_STATE.equals(flowState)) {
            return RESUME_FINISHED;
        } else if (flowState.toString().endsWith("FAILED_STATE")) {
            return RESUME_FAILED;
        } else {
            return UNSET;
        }
    }
}
