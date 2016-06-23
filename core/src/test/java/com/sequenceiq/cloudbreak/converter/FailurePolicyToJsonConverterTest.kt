package com.sequenceiq.cloudbreak.converter

import org.junit.Assert.assertEquals

import org.junit.Before
import org.junit.Test

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.api.model.FailurePolicyJson
import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.domain.FailurePolicy

class FailurePolicyToJsonConverterTest : AbstractEntityConverterTest<FailurePolicy>() {

    private var underTest: FailurePolicyToJsonConverter? = null

    @Before
    fun setUp() {
        underTest = FailurePolicyToJsonConverter()
    }

    @Test
    fun testConvert() {
        // GIVEN
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertEquals(AdjustmentType.BEST_EFFORT, result.adjustmentType)
        assertAllFieldsNotNull(result)
    }

    override fun createSource(): FailurePolicy {
        return TestUtil.failurePolicy()
    }
}
