package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.CREATE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.CREATE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.CREATE_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationEvent.START_EXTERNAL_DATABASE_CREATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.START_CREATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.cloud.config.CloudConfigValidationEvent.VALIDATE_CLOUD_CONFIG_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState;
import com.sequenceiq.cloudbreak.core.flow2.validate.cloud.config.CloudConfigValidationState;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class ProvisionFlowEventChainFactory implements FlowEventChainFactory<StackEvent>, ClusterUseCaseAware {
    @Override
    public String initEvent() {
        return FlowChainTriggers.FULL_PROVISION_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(StackEvent event) {

        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new StackEvent(VALIDATE_CLOUD_CONFIG_EVENT.event(), event.getResourceId(), event.accepted()));
        flowEventChain.add(new StackEvent(VALIDATE_KERBEROS_CONFIG_EVENT.event(), event.getResourceId(), event.accepted()));
        flowEventChain.add(new StackEvent(START_EXTERNAL_DATABASE_CREATION_EVENT.event(), event.getResourceId(), event.accepted()));
        flowEventChain.add(new ProvisionEvent(START_CREATION_EVENT.event(), event.getResourceId(), ProvisionType.REGULAR, event.accepted()));
        flowEventChain.add(new ProvisionEvent(CLUSTER_CREATION_EVENT.event(), event.getResourceId(), ProvisionType.REGULAR));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    @Override
    public OperationType getFlowOperationType() {
        return OperationType.PROVISION;
    }

    @Override
    public Value getUseCaseForFlowState(Enum flowState) {
        if (CloudConfigValidationState.INIT_STATE.equals(flowState)) {
            return CREATE_STARTED;
        } else if (ClusterCreationState.CLUSTER_CREATION_FINISHED_STATE.equals(flowState)) {
            return CREATE_FINISHED;
        } else if (flowState.toString().endsWith("FAILED_STATE")) {
            return CREATE_FAILED;
        } else {
            return UNSET;
        }
    }

}
