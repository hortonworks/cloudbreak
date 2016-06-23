package com.sequenceiq.cloudbreak.core.flow2

import java.util.ArrayList
import java.util.HashMap
import java.util.Optional

import javax.annotation.PostConstruct
import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.statemachine.StateContext
import org.springframework.statemachine.action.Action

import com.sequenceiq.cloudbreak.cloud.event.Payload
import com.sequenceiq.cloudbreak.cloud.event.Selectable

import reactor.bus.Event
import reactor.bus.EventBus

abstract class AbstractAction<S : FlowState, E : FlowEvent, C : CommonContext, P : Payload> protected constructor(private val payloadClass: Class<P>) : Action<S, E> {

    @Inject
    private val eventBus: EventBus? = null
    @Inject
    private val runningFlows: FlowRegister? = null
    private var payloadConverters: List<PayloadConverter<P>>? = null
    private var failureEvent: E? = null

    @PostConstruct
    fun init() {
        payloadConverters = ArrayList<PayloadConverter<P>>()
        initPayloadConverterMap(payloadConverters)
    }

    override fun execute(context: StateContext<S, E>) {
        val flowId = context.getMessageHeader(MessageFactory.HEADERS.FLOW_ID.name) as String
        val payload = convertPayload(context.getMessageHeader(MessageFactory.HEADERS.DATA.name))
        var flowContext: C? = null
        try {
            prepareExecution(payload, context.extendedState.variables)
            flowContext = createFlowContext(flowId, context, payload)
            doExecute(flowContext, payload, context.extendedState.variables)
        } catch (ex: Exception) {
            LOGGER.error("Error during execution of " + javaClass.getName(), ex)
            if (failureEvent != null) {
                sendEvent(flowId, failureEvent!!.stringRepresentation(), getFailurePayload(payload, Optional.ofNullable<C>(flowContext), ex))
            } else {
                LOGGER.error("Missing error handling for " + javaClass.getName())
            }
        }

    }

    fun setFailureEvent(failureEvent: E) {
        if (this.failureEvent != null && this.failureEvent != failureEvent) {
            throw UnsupportedOperationException("Failure event already configured. Actions reusable not allowed!")
        }
        this.failureEvent = failureEvent
    }

    protected fun getFlow(flowId: String): Flow {
        return runningFlows!!.get(flowId)
    }

    protected fun sendEvent(context: C) {
        val payload = createRequest(context)
        sendEvent(context.flowId, payload.selector(), payload)
    }

    protected fun sendEvent(flowId: String, payload: Selectable) {
        sendEvent(flowId, payload.selector(), payload)
    }

    protected fun sendEvent(flowId: String, selector: String, payload: Any) {
        LOGGER.info("Triggering event: {}", payload)
        val headers = HashMap<String, Any>()
        headers.put("FLOW_ID", flowId)
        val flowChainId = runningFlows!!.getFlowChainId(flowId)
        if (flowChainId != null) {
            headers.put("FLOW_CHAIN_ID", flowChainId)
        }
        eventBus!!.notify(selector, Event(Event.Headers(headers), payload))
    }

    protected open fun initPayloadConverterMap(payloadConverters: List<PayloadConverter<P>>) {
        // By default payloadconvertermap is empty.
    }

    protected open fun prepareExecution(payload: P, variables: Map<Any, Any>) {
    }

    protected open fun createRequest(context: C): Selectable {
        throw UnsupportedOperationException("Context based request creation is not supported by default")
    }

    protected abstract fun createFlowContext(flowId: String, stateContext: StateContext<S, E>, payload: P): C

    @Throws(Exception::class)
    protected abstract fun doExecute(context: C, payload: P, variables: Map<Any, Any>)

    protected abstract fun getFailurePayload(payload: P, flowContext: Optional<C>, ex: Exception): Any

    private fun convertPayload(payload: Any?): P {
        var result: P? = null
        try {
            if (payload == null || payloadClass.isAssignableFrom(payload.javaClass)) {
                result = payload as P?
            } else {
                for (payloadConverter in payloadConverters!!) {
                    if (payloadConverter.canConvert(payload.javaClass)) {
                        result = payloadConverter.convert(payload)
                        break
                    }
                }
                if (result == null) {
                    LOGGER.error("No payload converter found for {}, payload will be null", payload)
                }
            }
        } catch (ex: Exception) {
            LOGGER.error("Error happened during payload conversion, converted payload will be null! ", ex)
        }

        return result
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AbstractAction<FlowState, FlowEvent, CommonContext, Payload>::class.java)
    }
}
