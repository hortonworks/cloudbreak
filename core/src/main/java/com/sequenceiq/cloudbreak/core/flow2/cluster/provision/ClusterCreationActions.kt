package com.sequenceiq.cloudbreak.core.flow2.cluster.provision

import javax.inject.Inject

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.statemachine.action.Action

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterContext
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterRequest
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterSuccess
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariRequest
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesRequest
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesSuccess
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariSuccess

@Configuration
class ClusterCreationActions {
    @Inject
    private val clusterCreationService: ClusterCreationService? = null

    @Bean(name = "STARTING_AMBARI_SERVICES_STATE")
    fun startingAmbariServicesAction(): Action<Any, Any> {
        return object : AbstractClusterAction<StackEvent>(StackEvent::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: ClusterContext, payload: StackEvent, variables: Map<Any, Any>) {
                clusterCreationService!!.startingAmbariServices(context.stack, context.cluster)
                sendEvent(context)
            }

            override fun createRequest(context: ClusterContext): Selectable {
                return StartAmbariServicesRequest(context.stack.id)
            }
        }
    }

    @Bean(name = "STARTING_AMBARI_STATE")
    fun startingAmbariAction(): Action<Any, Any> {
        return object : AbstractClusterAction<StartAmbariServicesSuccess>(StartAmbariServicesSuccess::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: ClusterContext, payload: StartAmbariServicesSuccess, variables: Map<Any, Any>) {
                clusterCreationService!!.startingAmbari(context.stack)
                sendEvent(context)
            }

            override fun createRequest(context: ClusterContext): Selectable {
                return StartAmbariRequest(context.stack.id)
            }
        }
    }

    @Bean(name = "INSTALLING_CLUSTER_STATE")
    fun installingClusterAction(): Action<Any, Any> {
        return object : AbstractClusterAction<StartAmbariSuccess>(StartAmbariSuccess::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: ClusterContext, payload: StartAmbariSuccess, variables: Map<Any, Any>) {
                clusterCreationService!!.installingCluster(context.stack)
                sendEvent(context)
            }

            override fun createRequest(context: ClusterContext): Selectable {
                return InstallClusterRequest(context.stack.id)
            }
        }
    }

    @Bean(name = "CLUSTER_CREATION_FINISHED_STATE")
    fun clusterCreationFinishedAction(): Action<Any, Any> {
        return object : AbstractClusterAction<InstallClusterSuccess>(InstallClusterSuccess::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: ClusterContext, payload: InstallClusterSuccess, variables: Map<Any, Any>) {
                clusterCreationService!!.clusterInstallationFinished(context.stack, context.cluster)
                sendEvent(context)
            }

            override fun createRequest(context: ClusterContext): Selectable {
                return StackEvent(ClusterCreationEvent.CLUSTER_CREATION_FINISHED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    @Bean(name = "CLUSTER_CREATION_FAILED_STATE")
    fun clusterCreationFailedAction(): Action<Any, Any> {
        return object : AbstractStackFailureAction<ClusterCreationState, ClusterCreationEvent>() {
            @Throws(Exception::class)
            override fun doExecute(context: StackFailureContext, payload: StackFailureEvent, variables: Map<Any, Any>) {
                clusterCreationService!!.handleClusterCreationFailure(context.stack, payload.exception)
                sendEvent(context)
            }

            override fun createRequest(context: StackFailureContext): Selectable {
                return StackEvent(ClusterCreationEvent.CLUSTER_CREATION_FAILURE_HANDLED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }
}
