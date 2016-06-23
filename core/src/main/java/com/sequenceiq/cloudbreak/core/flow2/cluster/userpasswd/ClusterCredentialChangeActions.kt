package com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterCredentialChangeTriggerEvent
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterCredentialChangeRequest
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterCredentialChangeResult

@Configuration
class ClusterCredentialChangeActions {
    @Inject
    private val clusterCredentialChangeService: ClusterCredentialChangeService? = null

    @Bean(name = "CLUSTER_CREDENTIALCHANGE_STATE")
    fun changingClusterCredential(): Action<Any, Any> {
        return object : AbstractClusterAction<ClusterCredentialChangeTriggerEvent>(ClusterCredentialChangeTriggerEvent::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: ClusterContext, payload: ClusterCredentialChangeTriggerEvent, variables: Map<Any, Any>) {
                clusterCredentialChangeService!!.credentialChange(context.stack.id)
                sendEvent(context.flowId, ClusterCredentialChangeRequest(context.stack.id, payload.user, payload.password))
            }
        }
    }

    @Bean(name = "CLUSTER_CREDENTIALCHANGE_FINISHED_STATE")
    fun clusterCredentialChangeFinished(): Action<Any, Any> {
        return object : AbstractClusterAction<ClusterCredentialChangeResult>(ClusterCredentialChangeResult::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: ClusterContext, payload: ClusterCredentialChangeResult, variables: Map<Any, Any>) {
                clusterCredentialChangeService!!.finishCredentialChange(context.stack.id, context.cluster,
                        payload.request.user, payload.request.password)
                sendEvent(context)
            }

            override fun createRequest(context: ClusterContext): Selectable {
                return StackEvent(ClusterCredentialChangeEvent.FINALIZED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    @Bean(name = "CLUSTER_CREDENTIALCHANGE_FAILED_STATE")
    fun clusterCredentialChangeFailedAction(): Action<Any, Any> {
        return object : AbstractStackFailureAction<ClusterCredentialChangeState, ClusterCredentialChangeEvent>() {
            @Throws(Exception::class)
            override fun doExecute(context: StackFailureContext, payload: StackFailureEvent, variables: Map<Any, Any>) {
                LOGGER.error("Exception during cluster authentication change!: {}", payload.exception.message)
                sendEvent(context)
            }

            override fun createRequest(context: StackFailureContext): Selectable {
                return StackEvent(ClusterCredentialChangeEvent.FAIL_HANDLED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterCredentialChangeActions::class.java)
    }
}
