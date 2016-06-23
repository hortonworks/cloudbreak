package com.sequenceiq.cloudbreak.core.flow2.config

import org.junit.Assert.assertEquals
import org.mockito.BDDMockito.given

import java.util.ArrayList

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncFlowConfig
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig

class Flow2ConfigTest {

    @InjectMocks
    private var underTest: Flow2Config? = null

    @Mock
    private val flowConfigs: List<FlowConfiguration<*>>? = null

    @Before
    fun setUp() {
        underTest = Flow2Config()
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testFlowConfigurationMapInit() {
        val flowConfigs = ArrayList<FlowConfiguration<*>>()
        flowConfigs.add(StackSyncFlowConfig())
        flowConfigs.add(StackTerminationFlowConfig())
        given(this.flowConfigs!!.iterator()).willReturn(flowConfigs.iterator())
        val flowConfigMap = underTest!!.flowConfigurationMap()
        assertEquals("Not all flow type appeared in map!", countEvents(flowConfigs).toLong(), flowConfigMap.size.toLong())
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testFlowConfigurationMapInitIfAlreadyExists() {
        val flowConfigs = ArrayList<FlowConfiguration<*>>()
        val stackSyncFlowConfig = StackSyncFlowConfig()
        flowConfigs.add(stackSyncFlowConfig)
        flowConfigs.add(stackSyncFlowConfig)
        given(this.flowConfigs!!.iterator()).willReturn(flowConfigs.iterator())
        underTest!!.flowConfigurationMap()
    }

    private fun countEvents(flowConfigs: List<FlowConfiguration<*>>): Int {
        return flowConfigs.stream().mapToInt({ c -> c.initEvents.size }).sum()
    }
}
