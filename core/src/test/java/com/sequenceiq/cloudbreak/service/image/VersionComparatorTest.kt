package com.sequenceiq.cloudbreak.service.image

import java.io.IOException

import org.junit.Assert
import org.junit.Before
import org.junit.Test

import com.sequenceiq.cloudbreak.cloud.model.Versioned

class VersionComparatorTest {

    private var underTest: VersionComparator? = null

    @Before
    @Throws(IOException::class)
    fun setup() {
        underTest = VersionComparator()
    }


    @Test
    @Throws(IOException::class)
    fun testEquals() {
        Assert.assertEquals(0, underTest!!.compare(VersionString("2.4.0.0-770"), VersionString("2.4.0.0-770")).toLong())
    }

    @Test
    @Throws(IOException::class)
    fun testGreater() {
        Assert.assertEquals(1, underTest!!.compare(VersionString("2.4.0.0-880"), VersionString("2.4.0.0-770")).toLong())
        Assert.assertEquals(1, underTest!!.compare(VersionString("2.4.0.0-1000"), VersionString("2.4.0.0-770")).toLong())
        Assert.assertEquals(1, underTest!!.compare(VersionString("2.5.0.0-1000"), VersionString("2.4.0.0-1000")).toLong())
        Assert.assertEquals(1, underTest!!.compare(VersionString("2.15.0.0-1000"), VersionString("2.5.0.0-1000")).toLong())
    }


    @Test
    @Throws(IOException::class)
    fun testGreaterNonEqualLength() {
        Assert.assertEquals(1, underTest!!.compare(VersionString("2.4.0.0"), VersionString("2.4.0.0-770")).toLong())
        Assert.assertEquals(1, underTest!!.compare(VersionString("2.5.0.0"), VersionString("2.5.0.0-770")).toLong())
    }

    @Test
    @Throws(IOException::class)
    fun testSmaller() {
        Assert.assertEquals(-1, underTest!!.compare(VersionString("2.4.0.0-770"), VersionString("2.4.0.0-880")).toLong())
        Assert.assertEquals(-1, underTest!!.compare(VersionString("2.4.0.0-770"), VersionString("2.4.0.0-1000")).toLong())
        Assert.assertEquals(-1, underTest!!.compare(VersionString("2.4.0.0-1000"), VersionString("2.5.0.0-1000")).toLong())
        Assert.assertEquals(-1, underTest!!.compare(VersionString("2.5.0.0-1000"), VersionString("2.15.0.0-1000")).toLong())
    }

    @Test
    @Throws(IOException::class)
    fun testSmallerNonEqualLength() {
        Assert.assertEquals(-1, underTest!!.compare(VersionString("2.4.0.0"), VersionString("2.5.0.0-770")).toLong())
    }

    private inner class VersionString internal constructor(override val version: String) : Versioned

}
