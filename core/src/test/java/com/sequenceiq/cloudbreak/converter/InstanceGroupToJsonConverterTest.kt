package com.sequenceiq.cloudbreak.converter

import org.junit.Assert.assertEquals
import org.mockito.BDDMockito.given
import org.mockito.Matchers.any

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor

import com.google.common.collect.Sets
import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.api.model.InstanceStatus

class InstanceGroupToJsonConverterTest : AbstractEntityConverterTest<InstanceGroup>() {

    @InjectMocks
    private var underTest: InstanceGroupToJsonConverter? = null

    @Mock
    private val conversionService: ConversionService? = null

    @Before
    fun setUp() {
        underTest = InstanceGroupToJsonConverter()
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testConvert() {
        // GIVEN
        given(conversionService!!.convert(any<Any>(Any::class.java), any<TypeDescriptor>(TypeDescriptor::class.java), any<TypeDescriptor>(TypeDescriptor::class.java))).willReturn(getInstanceMetaData(source))
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertEquals(1, result.nodeCount.toLong())
        assertEquals(InstanceGroupType.CORE, result.type)
        assertAllFieldsNotNull(result)
    }

    override fun createSource(): InstanceGroup {
        val instanceGroup = TestUtil.instanceGroup(1L, InstanceGroupType.CORE, TestUtil.gcpTemplate(1L))
        instanceGroup.instanceMetaData = getInstanceMetaData(instanceGroup)
        return instanceGroup
    }

    private fun getInstanceMetaData(instanceGroup: InstanceGroup): Set<InstanceMetaData> {
        val metadata = TestUtil.instanceMetaData(1L, InstanceStatus.REGISTERED, false, instanceGroup)
        return Sets.newHashSet(metadata)
    }
}
