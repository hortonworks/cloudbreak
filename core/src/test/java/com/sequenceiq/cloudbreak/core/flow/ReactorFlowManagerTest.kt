package com.sequenceiq.cloudbreak.core.flow

import org.mockito.Matchers.any
import org.mockito.Matchers.anyObject
import org.mockito.Matchers.anyString
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import java.lang.reflect.Method

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner

import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson
import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.common.type.CloudConstants
import com.sequenceiq.cloudbreak.core.flow2.service.ErrorHandlerAwareFlowEventFactory
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager

import reactor.bus.Event
import reactor.bus.EventBus
import reactor.core.dispatch.ThreadPoolExecutorDispatcher

@RunWith(MockitoJUnitRunner::class)
class ReactorFlowManagerTest {

    @Mock
    private val reactor: EventBus? = null

    @Mock
    private val eventFactory: ErrorHandlerAwareFlowEventFactory? = null

    @InjectMocks
    private val flowManager: ReactorFlowManager? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        reset<EventBus>(reactor)
        reset<ErrorHandlerAwareFlowEventFactory>(eventFactory)
        `when`<EventBus>(reactor!!.notify(anyObject<Any>() as Any, any<Event>(Event<Any>::class.java))).thenReturn(EventBus(ThreadPoolExecutorDispatcher(1, 1)))
        `when`(eventFactory!!.createEvent(anyObject<Any>(), anyString())).thenReturn(Event<Any>(String::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun shouldReturnTheNextFailureTransition() {
        val stackId = 1L
        val instanceGroupAdjustment = InstanceGroupAdjustmentJson()
        val hostGroupAdjustment = HostGroupAdjustmentJson()

        flowManager!!.triggerProvisioning(stackId)
        flowManager.triggerClusterInstall(stackId)
        flowManager.triggerClusterReInstall(stackId)
        flowManager.triggerStackStop(stackId)
        flowManager.triggerStackStart(stackId)
        flowManager.triggerClusterStop(stackId)
        flowManager.triggerClusterStart(stackId)
        flowManager.triggerTermination(stackId)
        flowManager.triggerForcedTermination(stackId)
        flowManager.triggerStackUpscale(stackId, instanceGroupAdjustment)
        flowManager.triggerStackDownscale(stackId, instanceGroupAdjustment)
        flowManager.triggerStackRemoveInstance(stackId, "instanceId")
        flowManager.triggerClusterUpscale(stackId, hostGroupAdjustment)
        flowManager.triggerClusterDownscale(stackId, hostGroupAdjustment)
        flowManager.triggerClusterSync(stackId)
        flowManager.triggerStackSync(stackId)
        flowManager.triggerFullSync(stackId)
        flowManager.triggerClusterCredentialChange(stackId, "admin", "admin1")
        flowManager.triggerClusterTermination(stackId)

        var count = 0
        for (method in flowManager.javaClass.getDeclaredMethods()) {
            if (method.getName().startsWith("trigger")) {
                count++
            }
        }
        // Termination triggers flow cancellation
        count += 2
        verify<EventBus>(reactor, times(count)).notify(anyObject<Any>() as Any, any<Event>(Event<Any>::class.java))
    }

    companion object {
        private val GCP_PLATFORM = Platform.platform(CloudConstants.GCP)
    }
}
