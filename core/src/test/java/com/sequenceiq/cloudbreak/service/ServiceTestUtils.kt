package com.sequenceiq.cloudbreak.service

import com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS
import com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP

import java.util.Date
import java.util.HashSet

import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Credential
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.Resource
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.domain.Template

object ServiceTestUtils {

    val DUMMY_OWNER = "gipsz@jakab.kom"
    val DUMMY_ACCOUNT = "acmecorp"
    val PUBLIC_KEY = "mypublickey"
    private val C3LARGE_INSTANCE = "c3.large"
    private val N1_STANDARD_1 = "n1-standard-1"

    fun createBlueprint(owner: String, account: String): Blueprint {
        val blueprint = Blueprint()
        blueprint.id = 1L
        blueprint.blueprintName = "test-blueprint"
        blueprint.blueprintText = "dummyText"
        blueprint.hostGroupCount = 3
        blueprint.description = "test blueprint"
        blueprint.name = "multi-node-hdfs-yarn"
        blueprint.owner = owner
        blueprint.account = account
        blueprint.isPublicInAccount = true
        return blueprint
    }

    fun createStack(owner: String, account: String, template: Template, cluster: Cluster): Stack {
        return createStack(owner, account, template,
                createCredential(owner, account, template.cloudPlatform()),
                cluster)
    }

    @JvmOverloads fun createStack(template: Template, credential: Credential, resources: Set<Resource> = HashSet<Resource>()): Stack {
        return createStack(DUMMY_OWNER, DUMMY_ACCOUNT, template, credential, createCluster(), resources)
    }

    @JvmOverloads fun createStack(owner: String = DUMMY_OWNER, account: String = DUMMY_ACCOUNT, template: Template = createTemplate(owner, account, AWS), credential: Credential = createCredential(owner, account, AWS), cluster: Cluster = createCluster(owner, account, createBlueprint(owner, account)), resources: Set<Resource> = HashSet<Resource>()): Stack {
        val template1 = createTemplate(AWS)
        val template2 = createTemplate(AWS)
        val instanceGroups = HashSet<InstanceGroup>()
        val instanceGroup1 = InstanceGroup()
        instanceGroup1.nodeCount = 2
        instanceGroup1.groupName = "master"
        instanceGroup1.template = template1
        instanceGroups.add(instanceGroup1)
        val instanceGroup2 = InstanceGroup()
        instanceGroup2.nodeCount = 2
        instanceGroup2.groupName = "slave_1"
        instanceGroup2.template = template2
        instanceGroups.add(instanceGroup2)
        val stack = Stack()
        stack.credential = credential
        stack.region = "EU_WEST_1"
        stack.owner = owner
        stack.account = account
        stack.status = Status.REQUESTED
        stack.instanceGroups = instanceGroups
        stack.cluster = cluster
        stack.isPublicInAccount = true
        stack.resources = resources
        return stack
    }

    @JvmOverloads fun createCluster(owner: String = DUMMY_OWNER, account: String = DUMMY_ACCOUNT, blueprint: Blueprint = createBlueprint(DUMMY_OWNER, DUMMY_ACCOUNT)): Cluster {
        val cluster = Cluster()
        cluster.name = "test-cluster"
        cluster.description = "test cluster"
        cluster.emailNeeded = false
        cluster.ambariIp = "168.192.12.13"
        cluster.status = Status.AVAILABLE
        cluster.statusReason = ""
        cluster.creationStarted = 123456789L
        cluster.creationFinished = 223456789L
        cluster.owner = owner
        cluster.account = account
        cluster.blueprint = blueprint
        return cluster
    }

    fun createCredential(owner: String, account: String, platform: String): Credential? {
        when (platform) {
            AWS -> {
                val awsCredential = Credential()
                awsCredential.id = 1L
                awsCredential.owner = owner
                awsCredential.setCloudPlatform(platform)
                awsCredential.account = account
                awsCredential.isPublicInAccount = true
                awsCredential.publicKey = PUBLIC_KEY
                return awsCredential
            }
            GCP -> {
                val gcpCredential = Credential()
                gcpCredential.id = 1L
                gcpCredential.owner = owner
                gcpCredential.setCloudPlatform(platform)
                gcpCredential.account = account
                gcpCredential.isPublicInAccount = true
                gcpCredential.publicKey = PUBLIC_KEY
                return gcpCredential
            }
            else -> return null
        }
    }

    fun createTemplate(platform: String): Template {
        return createTemplate(DUMMY_OWNER, DUMMY_ACCOUNT, platform)
    }

    fun createTemplate(owner: String, account: String, platform: String): Template? {
        when (platform) {
            AWS -> {
                val awsTemplate = Template()
                awsTemplate.id = 1L
                awsTemplate.owner = owner
                awsTemplate.account = account
                awsTemplate.instanceType = C3LARGE_INSTANCE
                awsTemplate.volumeType = "gp2"
                awsTemplate.volumeCount = 1
                awsTemplate.volumeSize = 100
                awsTemplate.description = "aws test template"
                awsTemplate.isPublicInAccount = true
                awsTemplate.setCloudPlatform(AWS)
                return awsTemplate
            }
            GCP -> {
                val gcpTemplate = Template()
                gcpTemplate.id = 1L
                gcpTemplate.instanceType = N1_STANDARD_1
                gcpTemplate.volumeType = "pd-standard"
                gcpTemplate.description = "gcp test template"
                gcpTemplate.owner = owner
                gcpTemplate.account = account
                gcpTemplate.volumeCount = 1
                gcpTemplate.volumeSize = 100
                gcpTemplate.isPublicInAccount = true
                gcpTemplate.setCloudPlatform(GCP)
                return gcpTemplate
            }
            else -> return null
        }
    }

    fun createEvent(stackId: Long?, nodeCount: Int, eventStatus: String, eventTimestamp: Date): CloudbreakEvent {
        val event = CloudbreakEvent()
        event.stackId = stackId
        event.eventType = eventStatus
        event.eventTimestamp = eventTimestamp
        event.nodeCount = nodeCount
        return event
    }

}
