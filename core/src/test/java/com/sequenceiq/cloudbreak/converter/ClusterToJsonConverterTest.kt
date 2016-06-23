package com.sequenceiq.cloudbreak.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.mockito.BDDMockito.given
import org.mockito.Matchers.any
import org.mockito.Matchers.anySet
import org.mockito.Matchers.anyString
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

import java.io.IOException
import java.util.Arrays

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.core.convert.ConversionService

import com.fasterxml.jackson.databind.JsonNode
import com.google.common.collect.Sets
import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson
import com.sequenceiq.cloudbreak.api.model.ClusterResponse
import com.sequenceiq.cloudbreak.api.model.ConfigStrategy
import com.sequenceiq.cloudbreak.api.model.RDSConfigJson
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptor
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptors
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.SssdConfig
import com.sequenceiq.cloudbreak.domain.Stack

class ClusterToJsonConverterTest : AbstractEntityConverterTest<Cluster>() {

    @InjectMocks
    private var underTest: ClusterToJsonConverter? = null

    @Mock
    private val blueprintValidator: BlueprintValidator? = null
    @Mock
    private val stackServiceComponentDescs: StackServiceComponentDescriptors? = null
    @Mock
    private val conversionService: ConversionService? = null
    @Mock
    private val jsonNode: JsonNode? = null
    @Mock
    private val nameJsonNode: JsonNode? = null
    @Mock
    private val mockIterator: Iterator<JsonNode>? = null
    @Mock
    private val hostGroupMap: Map<String, HostGroup>? = null
    @Mock
    private val hostGroup: HostGroup? = null
    @Mock
    private val instanceGroup: InstanceGroup? = null
    @Mock
    private val instanceMetaData: InstanceMetaData? = null
    @Mock
    private val mockComponentIterator: Iterator<JsonNode>? = null

    private var stackServiceComponentDescriptor: StackServiceComponentDescriptor? = null

    @Before
    fun setUp() {
        underTest = ClusterToJsonConverter()
        MockitoAnnotations.initMocks(this)
        stackServiceComponentDescriptor = createStackServiceComponentDescriptor()
    }

    @Test
    @Throws(IOException::class)
    fun testConvert() {
        // GIVEN
        mockAll()
        source.configStrategy = ConfigStrategy.NEVER_APPLY
        given(stackServiceComponentDescs!!.get(anyString())).willReturn(stackServiceComponentDescriptor)
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertEquals(1L, result.id as Long)
        assertNotNull(result.ambariStackDetails)
        assertAllFieldsNotNull(result, Arrays.asList("cluster"))
    }

    @Test
    @Throws(IOException::class)
    fun testConvertWithoutAmbariStackDetails() {
        // GIVEN
        mockAll()
        source.ambariStackDetails = null
        source.configStrategy = ConfigStrategy.NEVER_APPLY
        given(stackServiceComponentDescs!!.get(anyString())).willReturn(stackServiceComponentDescriptor)
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertEquals(1L, result.id as Long)
        assertNull(result.ambariStackDetails)
        assertAllFieldsNotNull(result, Arrays.asList("ambariStackDetails", "cluster"))
    }

    @Test
    @Throws(IOException::class)
    fun testConvertWithoutUpSinceField() {
        // GIVEN
        mockAll()
        source.upSince = null
        given(stackServiceComponentDescs!!.get(anyString())).willReturn(stackServiceComponentDescriptor)
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertEquals(0L, result.minutesUp.toLong())
    }

    @Test
    @Throws(IOException::class)
    fun testConvertWithoutMasterComponent() {
        // GIVEN
        mockAll()
        given(stackServiceComponentDescs!!.get(anyString())).willReturn(StackServiceComponentDescriptor("dummy", "dummy", 1, 1))
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertEquals(1L, result.id as Long)
        assertNotNull(result.ambariStackDetails)
    }

    @Test
    @Throws(IOException::class)
    fun testConvertWhenValidatorThrowException() {
        // GIVEN
        given(blueprintValidator!!.getHostGroupNode(any<Blueprint>(Blueprint::class.java))).willThrow(IOException("error"))
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        verify(blueprintValidator, times(0)).createHostGroupMap(anySet())

    }

    override fun createSource(): Cluster {
        val stack = TestUtil.stack()
        val blueprint = TestUtil.blueprint()
        val config = TestUtil.sssdConfigs(1).iterator().next()
        return TestUtil.cluster(blueprint, config, stack, 1L)
    }

    @Throws(IOException::class)
    private fun mockAll() {
        given(blueprintValidator!!.getHostGroupNode(any<Blueprint>(Blueprint::class.java))).willReturn(jsonNode)
        given<Iterator<JsonNode>>(jsonNode!!.iterator()).willReturn(mockIterator)
        given(mockIterator!!.hasNext()).willReturn(true).willReturn(false)
        given(mockIterator.next()).willReturn(jsonNode)
        given(conversionService!!.convert<AmbariStackDetailsJson>(source.ambariStackDetails, AmbariStackDetailsJson::class.java)).willReturn(AmbariStackDetailsJson())
        given(conversionService.convert<RDSConfigJson>(source.rdsConfig, RDSConfigJson::class.java)).willReturn(RDSConfigJson())
        given(blueprintValidator.getHostGroupName(jsonNode)).willReturn("slave_1")
        given(blueprintValidator.createHostGroupMap(any<Set<Any>>(Set<Any>::class.java))).willReturn(hostGroupMap)
        given<HostGroup>(hostGroupMap!!["slave_1"]).willReturn(hostGroup)
        //TODO
        //        given(hostGroup.getInstanceGroup()).willReturn(instanceGroup);
        given(instanceGroup!!.instanceMetaData).willReturn(Sets.newHashSet<InstanceMetaData>(instanceMetaData))
        given(blueprintValidator.getComponentsNode(jsonNode)).willReturn(nameJsonNode)
        given<Iterator<JsonNode>>(nameJsonNode!!.iterator()).willReturn(mockComponentIterator)
        given(mockComponentIterator!!.hasNext()).willReturn(true).willReturn(false)
        given(mockComponentIterator.next()).willReturn(nameJsonNode)
        given(nameJsonNode.get(anyString())).willReturn(nameJsonNode)
        given(nameJsonNode.asText()).willReturn("dummyName")
    }

    private fun createStackServiceComponentDescriptor(): StackServiceComponentDescriptor {
        return StackServiceComponentDescriptor("ELASTIC_SEARCH", "MASTER", 1, 1)
    }
}
