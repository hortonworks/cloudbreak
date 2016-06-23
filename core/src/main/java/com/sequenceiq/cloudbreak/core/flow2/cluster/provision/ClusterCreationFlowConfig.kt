package com.sequenceiq.cloudbreak.core.flow2.cluster.provision

import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_INSTALL_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_FAILED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_FAILURE_HANDLED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.INSTALL_CLUSTER_FAILED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.INSTALL_CLUSTER_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.START_AMBARI_FAILED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.START_AMBARI_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.START_AMBARI_SERVICES_FAILED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.START_AMBARI_SERVICES_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CLUSTER_CREATION_FAILED_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CLUSTER_CREATION_FINISHED_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.FINAL_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.INIT_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.INSTALLING_CLUSTER_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.STARTING_AMBARI_SERVICES_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.STARTING_AMBARI_STATE

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration

@Component
class ClusterCreationFlowConfig : AbstractFlowConfiguration<ClusterCreationState, ClusterCreationEvent>(ClusterCreationState::class.java, ClusterCreationEvent::class.java) {

    override val flowTriggerCondition: ClusterCreationFlowTriggerCondition
        get() = applicationContext.getBean<ClusterCreationFlowTriggerCondition>(ClusterCreationFlowTriggerCondition::class.java)

    override val events: Array<ClusterCreationEvent>
        get() = ClusterCreationEvent.values()

    override val initEvents: Array<ClusterCreationEvent>
        get() = arrayOf(CLUSTER_CREATION_EVENT, CLUSTER_INSTALL_EVENT)

    protected override val transitions: List<AbstractFlowConfiguration.Transition<ClusterCreationState, ClusterCreationEvent>>
        get() = TRANSITIONS

    protected override val edgeConfig: AbstractFlowConfiguration.FlowEdgeConfig<ClusterCreationState, ClusterCreationEvent>
        get() = EDGE_CONFIG

    companion object {
        private val TRANSITIONS = Transition.Builder<ClusterCreationState, ClusterCreationEvent>().defaultFailureEvent(CLUSTER_CREATION_FAILED_EVENT).from(INIT_STATE).to(STARTING_AMBARI_SERVICES_STATE).event(CLUSTER_CREATION_EVENT).noFailureEvent().from(INIT_STATE).to(STARTING_AMBARI_STATE).event(CLUSTER_INSTALL_EVENT).noFailureEvent().from(STARTING_AMBARI_SERVICES_STATE).to(STARTING_AMBARI_STATE).event(START_AMBARI_SERVICES_FINISHED_EVENT).failureEvent(START_AMBARI_SERVICES_FAILED_EVENT).from(STARTING_AMBARI_STATE).to(INSTALLING_CLUSTER_STATE).event(START_AMBARI_FINISHED_EVENT).failureEvent(START_AMBARI_FAILED_EVENT).from(INSTALLING_CLUSTER_STATE).to(CLUSTER_CREATION_FINISHED_STATE).event(INSTALL_CLUSTER_FINISHED_EVENT).failureEvent(INSTALL_CLUSTER_FAILED_EVENT).from(CLUSTER_CREATION_FINISHED_STATE).to(FINAL_STATE).event(CLUSTER_CREATION_FINISHED_EVENT).defaultFailureEvent().build()

        private val EDGE_CONFIG = AbstractFlowConfiguration.FlowEdgeConfig(INIT_STATE, FINAL_STATE, CLUSTER_CREATION_FAILED_STATE, CLUSTER_CREATION_FAILURE_HANDLED_EVENT)
    }
}
