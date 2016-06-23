package com.sequenceiq.cloudbreak.cloud.init

import com.sequenceiq.cloudbreak.cloud.model.Platform.platform
import com.sequenceiq.cloudbreak.cloud.model.Variant.variant
import org.junit.Assert.assertEquals

import org.junit.Before
import org.junit.Test
import org.springframework.test.util.ReflectionTestUtils

import com.google.common.collect.Lists
import com.sequenceiq.cloudbreak.cloud.Authenticator
import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.CredentialConnector
import com.sequenceiq.cloudbreak.cloud.InstanceConnector
import com.sequenceiq.cloudbreak.cloud.MetadataCollector
import com.sequenceiq.cloudbreak.cloud.PlatformParameters
import com.sequenceiq.cloudbreak.cloud.ResourceConnector
import com.sequenceiq.cloudbreak.cloud.Setup
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.Variant

class CloudPlatformConnectorsTest {

    private val c = CloudPlatformConnectors()

    @Before
    fun setUp() {
        val connectorList = Lists.newArrayList<CloudConnector>()
        connectorList.add(getConnector("MULTIWITHDEFAULT", "ONE"))
        connectorList.add(getConnector("MULTIWITHDEFAULT", "TWO"))
        connectorList.add(getConnector("SINGLE", "SINGLE"))
        ReflectionTestUtils.setField(c, "cloudConnectors", connectorList)
        ReflectionTestUtils.setField(c, "platformDefaultVariants", "MULTIWITHDEFAULT:ONE")
        c.cloudPlatformConnectors()
    }

    @Test
    fun getDefaultForOpenstack() {
        val conn = c.getDefault(Companion.platform("MULTIWITHDEFAULT"))
        assertEquals("ONE", conn.variant().value())
    }

    @Test
    fun getDefaultForGcp() {
        val conn = c.getDefault(Companion.platform("SINGLE"))
        assertEquals("SINGLE", conn.variant().value())
    }

    @Test
    fun getOpenstackNative() {
        val conn = c.get(Companion.platform("MULTIWITHDEFAULT"), Companion.variant("TWO"))
        assertEquals("TWO", conn.variant().value())
    }

    @Test
    fun getWithNullVariant() {
        val conn = c.get(Companion.platform("MULTIWITHDEFAULT"), Companion.variant(null))
        //should fall back to default
        assertEquals("ONE", conn.variant().value())
    }

    @Test
    fun getWithEmptyVariant() {
        val conn = c.get(Companion.platform("MULTIWITHDEFAULT"), Companion.variant(""))
        //should fall back to default
        assertEquals("ONE", conn.variant().value())
    }

    @Test(expected = IllegalStateException::class)
    fun getConnectorDefaultWithNoDefault() {
        val connectorList = Lists.newArrayList<CloudConnector>()
        connectorList.add(getConnector("NODEFAULT", "ONE"))
        connectorList.add(getConnector("NODEFAULT", "TWO"))
        ReflectionTestUtils.setField(c, "cloudConnectors", connectorList)
        c.cloudPlatformConnectors()
    }

    private fun getConnector(platform: String, variant: String): CloudConnector {
        return object : CloudConnector {
            override fun authentication(): Authenticator {
                return null
            }

            override fun setup(): Setup {
                return null
            }

            override fun credentials(): CredentialConnector {
                return null
            }

            override fun resources(): ResourceConnector {
                return null
            }

            override fun instances(): InstanceConnector {
                return null
            }

            override fun metadata(): MetadataCollector {
                return null
            }

            override fun parameters(): PlatformParameters {
                return null
            }

            override fun variant(): Variant {
                return Variant.variant(variant)
            }

            override fun platform(): Platform {
                return Platform.platform(platform)
            }
        }
    }
}