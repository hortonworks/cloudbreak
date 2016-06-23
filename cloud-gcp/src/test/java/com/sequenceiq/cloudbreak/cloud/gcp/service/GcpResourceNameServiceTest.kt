package com.sequenceiq.cloudbreak.cloud.gcp.service

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.springframework.test.util.ReflectionTestUtils

import com.sequenceiq.cloudbreak.cloud.service.ResourceNameService
import com.sequenceiq.cloudbreak.common.type.ResourceType

class GcpResourceNameServiceTest {

    private var subject: ResourceNameService? = null
    private val maxResourceNameLength = "63"

    @Before
    @Throws(Exception::class)
    fun setUp() {
        subject = GcpResourceNameService()
        ReflectionTestUtils.setField(subject, "maxResourceNameLength", Integer.parseInt(maxResourceNameLength))
    }

    @Test
    @Throws(Exception::class)
    fun shouldGenerateNetworkResourceNameWhenStackNameProvided() {
        // GIVEN
        val parts = arrayOf("gcp Network")

        // WHEN
        val networkResourceName = subject!!.resourceName(ResourceType.GCP_NETWORK, *parts as Array<Any>)

        // THEN
        Assert.assertNotNull("The generated name must not be null!", networkResourceName)
        Assert.assertTrue("The timestamp must be appended", networkResourceName.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray().size == 2)
    }

    @Test
    @Throws(Exception::class)
    fun shouldGenerateFirewallInternalResourceNameWhenStackNameProvided() {
        // GIVEN
        val stackName = "dummy #stack_name?"

        // WHEN
        val resourceName = subject!!.resourceName(ResourceType.GCP_FIREWALL_INTERNAL, stackName)

        // THEN
        Assert.assertNotNull("The generated name must not be null!", resourceName)
        Assert.assertTrue("The timestamp must be appended", resourceName.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray().size == 3)
        Assert.assertEquals("The resource name suffix is not the expected one!", "internal", resourceName.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[1])
    }


    @Test
    @Throws(Exception::class)
    fun shouldGenerateFirewallInResourceNameWhenStackNameProvided() {
        // GIVEN
        val stackName = "dummy #stack_name?"

        // WHEN
        val resourceName = subject!!.resourceName(ResourceType.GCP_FIREWALL_IN, stackName)

        // THEN
        Assert.assertNotNull("The generated name must not be null!", resourceName)
        Assert.assertTrue("The timestamp must be appended", resourceName.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray().size == 3)
        Assert.assertEquals("The resource name suffix is not the expected one!", "in", resourceName.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[1])
    }

    @Test
    @Throws(Exception::class)
    fun shouldGenerateReservedIpResourceNameWhenStackNameProvided() {
        // GIVEN
        val stackName = "dummy #stack_name?"

        // WHEN
        val resourceName = subject!!.resourceName(ResourceType.GCP_RESERVED_IP, stackName)

        // THEN
        Assert.assertNotNull("The generated name must not be null!", resourceName)
        Assert.assertTrue("The timestamp must be appended", resourceName.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray().size == 4)
        Assert.assertEquals("The resource name suffix is not the expected one!", "reserved", resourceName.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[1])
    }

    @Test
    @Throws(Exception::class)
    fun shouldGenerateGcpAttachedDiskResourceWhenPartsProvided() {
        // GIVEN
        val parts = arrayOf("stack", "group", 3, 2)

        // WHEN
        val resourceName = subject!!.resourceName(ResourceType.GCP_ATTACHED_DISK, *parts)

        // THEN
        Assert.assertNotNull("The generated name must not be null!", resourceName)
        Assert.assertTrue("The timestamp must be appended", resourceName.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray().size == 5)
        Assert.assertTrue("The resource name is not the expected one!", resourceName.startsWith("stack-g-3-2"))


    }

    @Test
    @Throws(Exception::class)
    fun shouldShortenGcpInstanceNameWhenLongResourceNameProvided() {
        //GIVEN
        val parts = arrayOf("thisisaverylongtextwhichneedstobeshortenedbythespecificmethod", "thisisaverylonginstanceGroup", 8999)

        //WHEN
        val resourceName = subject!!.resourceName(ResourceType.GCP_INSTANCE, *parts)

        //THEN
        Assert.assertNotNull("The generated name must not be null!", resourceName)
        Assert.assertTrue("The timestamp must be appended!", resourceName.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray().size == 4)
        Assert.assertEquals("The resource name suffix is not the excepted one!", "thisisaverylongtextw", resourceName.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0])
        Assert.assertEquals("The instance group name is not the excepted one!", "t", resourceName.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[1])
        Assert.assertEquals("The private is not the excepted one!", "8999", resourceName.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[2])
        Assert.assertTrue("The resource name length is wrong", resourceName.length < Integer.parseInt(maxResourceNameLength))
    }

    @Test
    @Throws(Exception::class)
    fun shouldShortenGcpInstanceGroupNameWhenLongResourceNameProvided() {
        //GIVEN
        val parts = arrayOf("stackname", "thisisareallylonginstancenamewhichwillbeshortenedbythemethod", 8999)

        //WHEN

        val resourceName = subject!!.resourceName(ResourceType.GCP_INSTANCE, *parts)

        //THEN
        Assert.assertNotNull("The generated name must not be null!", resourceName)
        Assert.assertTrue("The timestamp must be appended!", resourceName.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray().size == 4)
        Assert.assertEquals("The resource name suffix is not the excepted one!", "stackname", resourceName.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0])
        Assert.assertEquals("The instance group name is not the excepted one!", "t", resourceName.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[1])
        Assert.assertEquals("The private is not the excepted one!", "8999", resourceName.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[2])
        Assert.assertTrue("The resource name length is wrong", resourceName.length < Integer.parseInt(maxResourceNameLength))

    }
}