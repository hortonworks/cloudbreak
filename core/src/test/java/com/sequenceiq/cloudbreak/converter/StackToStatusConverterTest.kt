package com.sequenceiq.cloudbreak.converter

import org.junit.Assert.assertEquals

import org.junit.Before
import org.junit.Test

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.api.model.Status

class StackToStatusConverterTest : AbstractEntityConverterTest<Stack>() {

    private var underTest: StackToStatusConverter? = null

    @Before
    fun setUp() {
        underTest = StackToStatusConverter()
    }

    @Test
    fun testConvert() {
        // GIVEN
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertEquals(1L, result["id"])
        assertEquals(Status.AVAILABLE.name, result["status"])
        assertEquals(Status.AVAILABLE.name, result["clusterStatus"])
    }

    override fun createSource(): Stack {
        val stack = TestUtil.stack()
        val cluster = TestUtil.cluster(TestUtil.blueprint(), stack, 1L)
        stack.cluster = cluster
        return stack
    }
}
