package com.sequenceiq.cloudbreak.service.usages

import com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS
import org.junit.Assert.assertEquals
import org.mockito.BDDMockito.given
import org.mockito.Matchers.any
import org.mockito.Matchers.anyLong

import java.text.ParseException
import java.util.ArrayList
import java.util.Date
import java.util.HashMap

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.test.util.ReflectionTestUtils

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.domain.Template
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.service.price.AwsPriceGenerator
import com.sequenceiq.cloudbreak.service.price.PriceGenerator

class IntervalStackUsageGeneratorTest {

    @InjectMocks
    private var underTest: IntervalStackUsageGenerator? = null

    @Mock
    private val stackRepository: StackRepository? = null

    @Mock
    private val instanceUsageGenerator: IntervalInstanceUsageGenerator? = null

    private var priceGenerators: MutableList<PriceGenerator>? = null

    private var cloudbreakEvent: CloudbreakEvent? = null

    private var stack: Stack? = null

    private var instanceHours: MutableMap<String, Long>? = null

    @Before
    fun setUp() {
        underTest = IntervalStackUsageGenerator()
        priceGenerators = ArrayList<PriceGenerator>()
        priceGenerators!!.add(AwsPriceGenerator())
        ReflectionTestUtils.setField(underTest, "priceGenerators", priceGenerators)
        stack = TestUtil.stack()
        cloudbreakEvent = TestUtil.gcpCloudbreakEvent(stack!!.id)
        instanceHours = HashMap<String, Long>()
        instanceHours!!.put("2012-12-12", 1L)
        MockitoAnnotations.initMocks(this)
    }

    @Test
    @Throws(ParseException::class)
    fun testGenerateUsages() {
        // GIVEN
        given(stackRepository!!.findById(anyLong())).willReturn(stack)
        given(instanceUsageGenerator!!.getInstanceHours(any<InstanceMetaData>(InstanceMetaData::class.java),
                any<Date>(Date::class.java), any<Date>(Date::class.java))).willReturn(instanceHours)
        // WHEN
        val result = underTest!!.generateUsages(DUMMY_START_DATE,
                DUMMY_END_DATE, cloudbreakEvent)
        // THEN
        assertEquals(3, result.size.toLong())
        assertEquals(1L, result[0].instanceHours!!.toLong())
    }

    @Test
    @Throws(ParseException::class)
    fun testGenerateUsagesWhenStackNotFound() {
        // GIVEN
        given(stackRepository!!.findById(anyLong())).willReturn(null)
        // WHEN
        val result = underTest!!.generateUsages(DUMMY_START_DATE,
                DUMMY_END_DATE, cloudbreakEvent)
        // THEN
        assertEquals(0, result.size.toLong())
    }

    @Test
    @Throws(ParseException::class)
    fun testGenerateUsagesWithAwsInstanceType() {
        // GIVEN
        for (instanceGroup in stack!!.instanceGroups) {
            val template = Template()
            template.setCloudPlatform(AWS)
            template.instanceType = C3XLARGE_INSTANCE
            instanceGroup.template = template
        }
        given(stackRepository!!.findById(anyLong())).willReturn(stack)
        given(instanceUsageGenerator!!.getInstanceHours(any<InstanceMetaData>(InstanceMetaData::class.java),
                any<Date>(Date::class.java), any<Date>(Date::class.java))).willReturn(instanceHours)
        // WHEN
        val result = underTest!!.generateUsages(DUMMY_START_DATE,
                DUMMY_END_DATE, cloudbreakEvent)
        // THEN
        assertEquals(3, result.size.toLong())
        assertEquals(1L, result[0].instanceHours!!.toLong())
    }

    @Test
    @Throws(ParseException::class)
    fun testGenerateUsagesWithGcpInstanceType() {
        // GIVEN
        for (instanceGroup in stack!!.instanceGroups) {
            val template = Template()
            template.setCloudPlatform(AWS)
            template.instanceType = C3XLARGE_INSTANCE
            instanceGroup.template = template
        }
        given(stackRepository!!.findById(anyLong())).willReturn(stack)
        given(instanceUsageGenerator!!.getInstanceHours(any<InstanceMetaData>(InstanceMetaData::class.java),
                any<Date>(Date::class.java), any<Date>(Date::class.java))).willReturn(instanceHours)
        // WHEN
        val result = underTest!!.generateUsages(DUMMY_START_DATE,
                DUMMY_END_DATE, cloudbreakEvent)
        // THEN
        assertEquals(3, result.size.toLong())
        assertEquals(1L, result[0].instanceHours!!.toLong())
    }

    @Test
    @Throws(ParseException::class)
    fun testGenerateUsagesWithOpenstackInstanceType() {
        // GIVEN
        for (instanceGroup in stack!!.instanceGroups) {
            val template = Template()
            template.setCloudPlatform(AWS)
            template.instanceType = C3XLARGE_INSTANCE
            instanceGroup.template = template
        }
        given(stackRepository!!.findById(anyLong())).willReturn(stack)
        given(instanceUsageGenerator!!.getInstanceHours(any<InstanceMetaData>(InstanceMetaData::class.java),
                any<Date>(Date::class.java), any<Date>(Date::class.java))).willReturn(instanceHours)
        // WHEN
        val result = underTest!!.generateUsages(DUMMY_START_DATE,
                DUMMY_END_DATE, cloudbreakEvent)
        // THEN
        assertEquals(3, result.size.toLong())
        assertEquals(1L, result[0].instanceHours!!.toLong())
    }

    @Test
    @Throws(ParseException::class)
    fun testGenerateUsagesWithoutPriceGenerator() {
        // GIVEN
        priceGenerators!!.clear()
        given(stackRepository!!.findById(anyLong())).willReturn(stack)
        // WHEN
        val result = underTest!!.generateUsages(DUMMY_START_DATE,
                DUMMY_END_DATE, cloudbreakEvent)
        // THEN
        assertEquals(0, result.size.toLong())
    }

    companion object {

        private val DUMMY_START_DATE = Date()
        private val DUMMY_END_DATE = Date()
        private val C3XLARGE_INSTANCE = "c3.xlarge"
    }
}
