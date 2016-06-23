package com.sequenceiq.cloudbreak.cloud.handler

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone
import com.sequenceiq.cloudbreak.cloud.model.Location.location
import com.sequenceiq.cloudbreak.cloud.model.Region.region

import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap

import org.springframework.stereotype.Component

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate
import com.sequenceiq.cloudbreak.cloud.model.Location
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.model.Security
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule
import com.sequenceiq.cloudbreak.cloud.model.Subnet
import com.sequenceiq.cloudbreak.cloud.model.Volume
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Component
class ParameterGenerator {

    fun createCloudContext(): CloudContext {
        val location = Companion.location(Companion.region("region"), Companion.availabilityZone("availabilityZone"))
        return CloudContext(STACK_ID, "teststack", "TESTCONNECTOR", "owner", "TESTVARIANT", location)
    }

    fun createCloudCredential(): CloudCredential {
        val c = CloudCredential(1L, "opencred", "public_key", "cloudbreak")
        c.putParameter("userName", "userName")
        c.putParameter("password", "password")
        c.putParameter("tenantName", "tenantName")
        c.putParameter("endpoint", "http://endpoint:8080/v2.0")

        return c
    }

    fun createCloudStack(): CloudStack {
        val groups = ArrayList<Group>()

        val name = "master"
        val volumes = Arrays.asList(Volume("/hadoop/fs1", "HDD", 1), Volume("/hadoop/fs2", "HDD", 1))
        val instanceTemplate = InstanceTemplate("m1.medium", name, 0L, volumes, InstanceStatus.CREATE_REQUESTED,
                HashMap<String, Any>())

        val instance = CloudInstance("SOME_ID", instanceTemplate)

        groups.add(Group(name, InstanceGroupType.CORE, Arrays.asList(instance)))

        val userData = ImmutableMap.of(
                InstanceGroupType.CORE, "CORE",
                InstanceGroupType.GATEWAY, "GATEWAY")
        val image = Image("cb-centos66-amb200-2015-05-25", userData, null, null)

        val subnet = Subnet("10.0.0.0/24")
        val network = Network(subnet)
        network.putParameter("publicNetId", "028ffc0c-63c5-4ca0-802a-3ac753eaf76c")

        val rules = Arrays.asList(SecurityRule("0.0.0.0/0", arrayOf("22", "443"), "tcp"))
        val security = Security(rules)

        return CloudStack(groups, network, security, image, HashMap<String, String>())
    }

    val sshFingerprint: String
        get() = "43:51:43:a1:b5:fc:8b:b7:0a:3a:a9:b1:0f:66:73:a8"

    fun createCloudResourceList(): List<CloudResource> {
        val cr = CloudResource.Builder().type(ResourceType.HEAT_STACK).name("testref").build()
        return Lists.newArrayList(cr)
    }

    fun createCloudInstances(): List<CloudInstance> {
        return Lists.newArrayList<CloudInstance>()
    }


    fun createLaunchStackRequest(): LaunchStackRequest {
        return LaunchStackRequest(createCloudContext(), createCloudCredential(), createCloudStack(), AdjustmentType.BEST_EFFORT, 0L)
    }

    fun createAuthenticatedContext(): AuthenticatedContext {
        return AuthenticatedContext(createCloudContext(), createCloudCredential())
    }

    companion object {

        private val STACK_ID = 5L
    }
}
