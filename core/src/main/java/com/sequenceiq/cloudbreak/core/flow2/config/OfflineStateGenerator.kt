package com.sequenceiq.cloudbreak.core.flow2.config

import java.io.File
import java.io.IOException
import java.lang.reflect.Field
import java.nio.file.Files
import java.nio.file.Paths
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.Optional

import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.statemachine.StateContext
import org.springframework.statemachine.StateMachine
import org.springframework.statemachine.transition.Transition

import com.sequenceiq.cloudbreak.cloud.event.Payload
import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.CommonContext
import com.sequenceiq.cloudbreak.core.flow2.Flow
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowState
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleFlowConfig
import com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetFlowConfig
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartFlowConfig
import com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopFlowConfig
import com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncFlowConfig
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationFlowConfig
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleFlowConfig
import com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeFlowConfig
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleConfig
import com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationFlowConfig
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationFlowConfig
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartFlowConfig
import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopFlowConfig
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncFlowConfig
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleConfig

class OfflineStateGenerator private constructor(private val flowConfiguration: FlowConfiguration<FlowEvent>) {

    @Throws(Exception::class)
    private fun generate() {
        val builder = StringBuilder("digraph {\n")
        injectAppContext(flowConfiguration)
        val flow = initializeFlow()
        val stateMachine = getStateMachine(flow)
        val init = stateMachine.initialState.id
        builder.append(generateStartPoint(init, flowConfiguration.javaClass.getSimpleName())).append("\n")
        val transitions = stateMachine.transitions as List<Transition<FlowState, FlowEvent>>
        val transitionsAlreadyDefined = HashMap<String, FlowState>()
        transitionsAlreadyDefined.put(init.toString(), init)
        while (!transitions.isEmpty()) {
            for (transition in ArrayList(transitions)) {
                val source = transition.source.id
                val target = transition.target.id
                if (transitionsAlreadyDefined.values.contains(source)) {
                    val id = generateTransitionId(source, target, transition.trigger.event)
                    if (!transitionsAlreadyDefined.keys.contains(id)) {
                        if (target.action() != null && !transitionsAlreadyDefined.values.contains(target)) {
                            builder.append(generateState(target, target.action().simpleName)).append("\n")
                        }
                        builder.append(generateTransition(source, target, transition.trigger.event)).append("\n")
                        transitionsAlreadyDefined.put(id, target)
                    }
                    transitions.remove(transition)
                }
            }
        }
        saveToFile(builder.append("}").toString())
    }

    private fun generateTransitionId(source: FlowState, target: FlowState, event: FlowEvent): String {
        return source.toString() + target.toString() + event.toString()
    }

    private fun generateStartPoint(name: FlowState, label: String): String {
        return String.format("%s [label=\"%s\" shape=ellipse color=green];", name, label)
    }

    private fun generateState(state: FlowState, action: String): String {
        return String.format("%s [label=\"%s\\n%s\" shape=rect color=black];", state, state, action)
    }

    private fun generateTransition(source: FlowState, target: FlowState, event: FlowEvent): String {
        var color = "black"
        var style = "solid"
        if (source === target) {
            color = "blue"
        } else if (event.name().indexOf("FAIL") != -1 || event.name().indexOf("ERROR") != -1) {
            color = "red"
            style = "dashed"
        }
        return String.format("%s -> %s [label=\"%s\" color=%s style=%s];", source, target, event, color, style)
    }

    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    private fun getStateMachine(flow: Flow): StateMachine<FlowState, FlowEvent> {
        val flowMachine = flow.javaClass.getDeclaredField("flowMachine")
        flowMachine.setAccessible(true)
        return flowMachine.get(flow)
    }

    @Throws(Exception::class)
    private fun initializeFlow(): Flow {
        (flowConfiguration as AbstractFlowConfiguration<FlowState, FlowEvent>).init()
        val flow = flowConfiguration.createFlow("")
        flow.initialize()
        return flow
    }

    @Throws(IOException::class)
    private fun saveToFile(content: String) {
        val destinationDir = File(OUT_PATH)
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }
        Files.write(Paths.get(String.format("%s/%s.dot", OUT_PATH, flowConfiguration.javaClass.getSimpleName())), content.toByteArray())
    }

    internal class CustomApplicationContext : AbstractApplicationContext() {

        @Throws(BeansException::class)
        override fun <T> getBean(name: String, requiredType: Class<T>): T {
            return CustomAction() as T
        }

        @Throws(BeansException::class)
        override fun refreshBeanFactory() {

        }

        override fun closeBeanFactory() {

        }

        override fun getBeanFactory(): ConfigurableListableBeanFactory? {
            return null
        }
    }

    internal class CustomAction : AbstractAction<FlowState, FlowEvent, CommonContext, Payload>(Payload::class.java) {

        override fun execute(context: StateContext<FlowState, FlowEvent>) {
        }

        override fun createFlowContext(flowId: String, stateContext: StateContext<FlowState, FlowEvent>, payload: Payload): CommonContext? {
            return null
        }

        @Throws(Exception::class)
        override fun doExecute(context: CommonContext, payload: Payload, variables: Map<Any, Any>) {

        }

        override fun createRequest(context: CommonContext): Selectable? {
            return null
        }

        override fun getFailurePayload(payload: Payload, flowContext: Optional<CommonContext>, ex: Exception): Any? {
            return null
        }
    }

    companion object {

        private val OUT_PATH = "build/diagrams/flow"

        private val CONFIGS = Arrays.asList<FlowConfiguration<out FlowEvent>>(
                ClusterTerminationFlowConfig(),
                InstanceTerminationFlowConfig(),
                StackCreationFlowConfig(),
                StackStartFlowConfig(),
                StackStopFlowConfig(),
                StackSyncFlowConfig(),
                ClusterSyncFlowConfig(),
                StackUpscaleConfig(),
                ClusterDownscaleFlowConfig(),
                StackDownscaleConfig(),
                StackTerminationFlowConfig(),
                ClusterUpscaleFlowConfig(),
                ClusterStartFlowConfig(),
                ClusterStopFlowConfig(),
                ClusterResetFlowConfig(),
                ClusterCredentialChangeFlowConfig())

        private val APP_CONTEXT = CustomApplicationContext()

        @Throws(Exception::class)
        @JvmStatic fun main(args: Array<String>) {
            for (flowConfiguration in CONFIGS) {
                OfflineStateGenerator(flowConfiguration).generate()
            }
        }

        @Throws(IllegalAccessException::class, NoSuchFieldException::class)
        private fun injectAppContext(flowConfiguration: FlowConfiguration<FlowEvent>) {
            val applicationContext = flowConfiguration.javaClass.getSuperclass().getDeclaredField("applicationContext")
            applicationContext.setAccessible(true)
            applicationContext.set(flowConfiguration, APP_CONTEXT)
        }
    }
}
