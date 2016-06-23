package com.sequenceiq.cloudbreak.cloud.aws

import org.mockito.Matchers.any
import org.mockito.Matchers.eq
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import java.util.ArrayList
import java.util.Arrays

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.runners.MockitoJUnitRunner

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.CreateTagsRequest
import com.amazonaws.services.ec2.model.DescribeInstancesRequest
import com.amazonaws.services.ec2.model.DescribeInstancesResult
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.Reservation
import com.amazonaws.services.ec2.model.Tag
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate
import com.sequenceiq.cloudbreak.cloud.model.Location
import com.sequenceiq.cloudbreak.cloud.model.Region
import com.sequenceiq.cloudbreak.cloud.model.Volume
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler


@RunWith(MockitoJUnitRunner::class)
class AwsMetaDataCollectorTest {

    @Mock
    private val awsClient: AwsClient? = null

    @Mock
    private val cloudFormationStackUtil: CloudFormationStackUtil? = null

    @Mock
    private val syncPollingScheduler: SyncPollingScheduler<Boolean>? = null

    @Mock
    private val awsPollTaskFactory: AwsPollTaskFactory? = null

    @Mock
    private val amazonCFClient: AmazonCloudFormationClient? = null

    @Mock
    private val amazonASClient: AmazonAutoScalingClient? = null

    @Mock
    private val amazonEC2Client: AmazonEC2Client? = null

    @Mock
    private val describeInstancesRequestGw: DescribeInstancesRequest? = null

    @Mock
    private val describeInstancesRequestMaster: DescribeInstancesRequest? = null

    @Mock
    private val describeInstancesRequestSlave: DescribeInstancesRequest? = null

    @Mock
    private val describeInstancesResultGw: DescribeInstancesResult? = null

    @Mock
    private val describeInstancesResultMaster: DescribeInstancesResult? = null

    @Mock
    private val describeInstancesResultSlave: DescribeInstancesResult? = null

    @InjectMocks
    private val awsMetadataCollector: AwsMetadataCollector? = null

    @Before
    fun setUp() {
        Mockito.reset<AmazonEC2Client>(amazonEC2Client)
    }


    @Test
    fun collectMigratedExistingOneGroup() {
        val vms = ArrayList<CloudInstance>()
        val volumes = ArrayList<Volume>()
        vms.add(CloudInstance("i-1", InstanceTemplate("fla", "cbgateway", 5L, volumes, InstanceStatus.CREATED, null)))


        `when`(awsClient!!.createCloudFormationClient(any<AwsCredentialView>(AwsCredentialView::class.java), eq("region"))).thenReturn(amazonCFClient)
        `when`(awsClient.createAutoScalingClient(any<AwsCredentialView>(AwsCredentialView::class.java), eq("region"))).thenReturn(amazonASClient)
        `when`(awsClient.createAccess(any<AwsCredentialView>(AwsCredentialView::class.java), eq("region"))).thenReturn(amazonEC2Client)

        `when`(cloudFormationStackUtil!!.getAutoscalingGroupName(any<AuthenticatedContext>(AuthenticatedContext::class.java), any<AmazonCloudFormationClient>(AmazonCloudFormationClient::class.java), eq("cbgateway"))).thenReturn("cbgateway-AAA")

        val gatewayIds = Arrays.asList("i-1")
        `when`(cloudFormationStackUtil.getInstanceIds(any<AmazonAutoScalingClient>(AmazonAutoScalingClient::class.java), eq("cbgateway-AAA"))).thenReturn(gatewayIds)

        `when`(cloudFormationStackUtil.createDescribeInstancesRequest(eq(gatewayIds))).thenReturn(describeInstancesRequestGw)

        `when`(amazonEC2Client!!.describeInstances(describeInstancesRequestGw)).thenReturn(describeInstancesResultGw)

        val instance = Mockito.mock<Instance>(Instance::class.java)
        `when`(instance.instanceId).thenReturn("i-1")
        `when`(instance.privateIpAddress).thenReturn("privateIp")
        `when`(instance.publicIpAddress).thenReturn("publicIp")

        val gatewayReservations = Arrays.asList(getReservation(instance))

        `when`(describeInstancesResultGw!!.reservations).thenReturn(gatewayReservations)

        val ac = authenticatedContext()
        val statuses = awsMetadataCollector!!.collect(ac, null, vms)

        verify(amazonEC2Client).createTags(any<CreateTagsRequest>(CreateTagsRequest::class.java))
        Assert.assertEquals(1, statuses.size.toLong())
        Assert.assertEquals("i-1", statuses[0].cloudVmInstanceStatus.cloudInstance.instanceId)
        Assert.assertEquals("privateIp", statuses[0].metaData.privateIp)
        Assert.assertEquals("publicIp", statuses[0].metaData.publicIp)
    }

    @Test
    fun collectAlreadyTaggedOneGroup() {
        val vms = ArrayList<CloudInstance>()
        val volumes = ArrayList<Volume>()
        vms.add(CloudInstance("i-1", InstanceTemplate("fla", "cbgateway", 5L, volumes, InstanceStatus.CREATED, null)))


        `when`(awsClient!!.createCloudFormationClient(any<AwsCredentialView>(AwsCredentialView::class.java), eq("region"))).thenReturn(amazonCFClient)
        `when`(awsClient.createAutoScalingClient(any<AwsCredentialView>(AwsCredentialView::class.java), eq("region"))).thenReturn(amazonASClient)
        `when`(awsClient.createAccess(any<AwsCredentialView>(AwsCredentialView::class.java), eq("region"))).thenReturn(amazonEC2Client)

        `when`(cloudFormationStackUtil!!.getAutoscalingGroupName(any<AuthenticatedContext>(AuthenticatedContext::class.java), any<AmazonCloudFormationClient>(AmazonCloudFormationClient::class.java), eq("cbgateway"))).thenReturn("cbgateway-AAA")

        val gatewayIds = Arrays.asList("i-1")
        `when`(cloudFormationStackUtil.getInstanceIds(any<AmazonAutoScalingClient>(AmazonAutoScalingClient::class.java), eq("cbgateway-AAA"))).thenReturn(gatewayIds)

        `when`(cloudFormationStackUtil.createDescribeInstancesRequest(eq(gatewayIds))).thenReturn(describeInstancesRequestGw)

        `when`(amazonEC2Client!!.describeInstances(describeInstancesRequestGw)).thenReturn(describeInstancesResultGw)

        val instance = Mockito.mock<Instance>(Instance::class.java)
        `when`(instance.instanceId).thenReturn("i-1")
        `when`(instance.privateIpAddress).thenReturn("privateIp")
        `when`(instance.publicIpAddress).thenReturn("publicIp")
        val tag = Tag()
        tag.key = "cbname"
        tag.value = "somevalue"
        `when`(instance.tags).thenReturn(Arrays.asList(tag))

        val gatewayReservations = Arrays.asList(getReservation(instance))

        `when`(describeInstancesResultGw!!.reservations).thenReturn(gatewayReservations)

        val ac = authenticatedContext()
        val statuses = awsMetadataCollector!!.collect(ac, null, vms)

        verify(amazonEC2Client, never()).createTags(any<CreateTagsRequest>(CreateTagsRequest::class.java))
        Assert.assertEquals(1, statuses.size.toLong())
        Assert.assertEquals("i-1", statuses[0].cloudVmInstanceStatus.cloudInstance.instanceId)
        Assert.assertEquals("privateIp", statuses[0].metaData.privateIp)
        Assert.assertEquals("publicIp", statuses[0].metaData.publicIp)
    }


    @Test
    fun collectNewOneGroup() {
        val vms = ArrayList<CloudInstance>()
        val volumes = ArrayList<Volume>()
        vms.add(CloudInstance(null, InstanceTemplate("fla", "cbgateway", 5L, volumes, InstanceStatus.CREATED, null)))


        `when`(awsClient!!.createCloudFormationClient(any<AwsCredentialView>(AwsCredentialView::class.java), eq("region"))).thenReturn(amazonCFClient)
        `when`(awsClient.createAutoScalingClient(any<AwsCredentialView>(AwsCredentialView::class.java), eq("region"))).thenReturn(amazonASClient)
        `when`(awsClient.createAccess(any<AwsCredentialView>(AwsCredentialView::class.java), eq("region"))).thenReturn(amazonEC2Client)

        `when`(cloudFormationStackUtil!!.getAutoscalingGroupName(any<AuthenticatedContext>(AuthenticatedContext::class.java), any<AmazonCloudFormationClient>(AmazonCloudFormationClient::class.java), eq("cbgateway"))).thenReturn("cbgateway-AAA")

        val gatewayIds = Arrays.asList("i-1")
        `when`(cloudFormationStackUtil.getInstanceIds(any<AmazonAutoScalingClient>(AmazonAutoScalingClient::class.java), eq("cbgateway-AAA"))).thenReturn(gatewayIds)

        `when`(cloudFormationStackUtil.createDescribeInstancesRequest(eq(gatewayIds))).thenReturn(describeInstancesRequestGw)

        `when`(amazonEC2Client!!.describeInstances(describeInstancesRequestGw)).thenReturn(describeInstancesResultGw)

        val instance = Mockito.mock<Instance>(Instance::class.java)
        `when`(instance.instanceId).thenReturn("i-1")
        `when`(instance.privateIpAddress).thenReturn("privateIp")
        `when`(instance.publicIpAddress).thenReturn("publicIp")

        val gatewayReservations = Arrays.asList(getReservation(instance))

        `when`(describeInstancesResultGw!!.reservations).thenReturn(gatewayReservations)

        val ac = authenticatedContext()
        val statuses = awsMetadataCollector!!.collect(ac, null, vms)

        verify(amazonEC2Client).createTags(any<CreateTagsRequest>(CreateTagsRequest::class.java))
        Assert.assertEquals(1, statuses.size.toLong())
        Assert.assertEquals("i-1", statuses[0].cloudVmInstanceStatus.cloudInstance.instanceId)
        Assert.assertEquals("privateIp", statuses[0].metaData.privateIp)
        Assert.assertEquals("publicIp", statuses[0].metaData.publicIp)
    }


    @Test
    fun collectNewAndExistingOneGroup() {
        val vms = ArrayList<CloudInstance>()
        val volumes = ArrayList<Volume>()
        vms.add(CloudInstance(null, InstanceTemplate("fla", "cbgateway", 5L, volumes, InstanceStatus.CREATED, null)))
        vms.add(CloudInstance("i-1", InstanceTemplate("fla", "cbgateway", 5L, volumes, InstanceStatus.CREATED, null)))


        `when`(awsClient!!.createCloudFormationClient(any<AwsCredentialView>(AwsCredentialView::class.java), eq("region"))).thenReturn(amazonCFClient)
        `when`(awsClient.createAutoScalingClient(any<AwsCredentialView>(AwsCredentialView::class.java), eq("region"))).thenReturn(amazonASClient)
        `when`(awsClient.createAccess(any<AwsCredentialView>(AwsCredentialView::class.java), eq("region"))).thenReturn(amazonEC2Client)

        `when`(cloudFormationStackUtil!!.getAutoscalingGroupName(any<AuthenticatedContext>(AuthenticatedContext::class.java), any<AmazonCloudFormationClient>(AmazonCloudFormationClient::class.java), eq("cbgateway"))).thenReturn("cbgateway-AAA")

        val gatewayIds = Arrays.asList("i-1", "i-2")
        `when`(cloudFormationStackUtil.getInstanceIds(any<AmazonAutoScalingClient>(AmazonAutoScalingClient::class.java), eq("cbgateway-AAA"))).thenReturn(gatewayIds)

        `when`(cloudFormationStackUtil.createDescribeInstancesRequest(eq(gatewayIds))).thenReturn(describeInstancesRequestGw)

        `when`(amazonEC2Client!!.describeInstances(describeInstancesRequestGw)).thenReturn(describeInstancesResultGw)

        val instance1 = Mockito.mock<Instance>(Instance::class.java)
        `when`(instance1.instanceId).thenReturn("i-1")
        `when`(instance1.privateIpAddress).thenReturn("privateIp1")
        `when`(instance1.publicIpAddress).thenReturn("publicIp1")

        val instance2 = Mockito.mock<Instance>(Instance::class.java)
        `when`(instance2.instanceId).thenReturn("i-2")
        `when`(instance2.privateIpAddress).thenReturn("privateIp2")
        `when`(instance2.publicIpAddress).thenReturn("publicIp2")

        val gatewayReservations = Arrays.asList(getReservation(instance1, instance2))

        `when`(describeInstancesResultGw!!.reservations).thenReturn(gatewayReservations)

        val ac = authenticatedContext()
        val statuses = awsMetadataCollector!!.collect(ac, null, vms)

        verify(amazonEC2Client, times(2)).createTags(any<CreateTagsRequest>(CreateTagsRequest::class.java))
        Assert.assertEquals(2, statuses.size.toLong())
        Assert.assertEquals("i-1", statuses[0].cloudVmInstanceStatus.cloudInstance.instanceId)
        Assert.assertEquals("privateIp1", statuses[0].metaData.privateIp)
        Assert.assertEquals("publicIp1", statuses[0].metaData.publicIp)

        Assert.assertEquals("i-2", statuses[1].cloudVmInstanceStatus.cloudInstance.instanceId)
        Assert.assertEquals("privateIp2", statuses[1].metaData.privateIp)
        Assert.assertEquals("publicIp2", statuses[1].metaData.publicIp)
    }

    @Test
    fun collectNewOldIsTagged() {
        val vms = ArrayList<CloudInstance>()
        val volumes = ArrayList<Volume>()
        vms.add(CloudInstance(null, InstanceTemplate("fla", "cbgateway", 5L, volumes, InstanceStatus.CREATED, null)))

        `when`(awsClient!!.createCloudFormationClient(any<AwsCredentialView>(AwsCredentialView::class.java), eq("region"))).thenReturn(amazonCFClient)
        `when`(awsClient.createAutoScalingClient(any<AwsCredentialView>(AwsCredentialView::class.java), eq("region"))).thenReturn(amazonASClient)
        `when`(awsClient.createAccess(any<AwsCredentialView>(AwsCredentialView::class.java), eq("region"))).thenReturn(amazonEC2Client)

        `when`(cloudFormationStackUtil!!.getAutoscalingGroupName(any<AuthenticatedContext>(AuthenticatedContext::class.java), any<AmazonCloudFormationClient>(AmazonCloudFormationClient::class.java), eq("cbgateway"))).thenReturn("cbgateway-AAA")

        val gatewayIds = Arrays.asList("i-1", "i-new")
        `when`(cloudFormationStackUtil.getInstanceIds(any<AmazonAutoScalingClient>(AmazonAutoScalingClient::class.java), eq("cbgateway-AAA"))).thenReturn(gatewayIds)

        `when`(cloudFormationStackUtil.createDescribeInstancesRequest(eq(gatewayIds))).thenReturn(describeInstancesRequestGw)

        `when`(amazonEC2Client!!.describeInstances(describeInstancesRequestGw)).thenReturn(describeInstancesResultGw)

        val instance1 = Mockito.mock<Instance>(Instance::class.java)
        `when`(instance1.instanceId).thenReturn("i-1")
        `when`(instance1.privateIpAddress).thenReturn("privateIp1")
        `when`(instance1.publicIpAddress).thenReturn("publicIp1")
        val tag = Tag()
        tag.key = "cbname"
        tag.value = "somevalue"
        `when`(instance1.tags).thenReturn(Arrays.asList(tag))

        val instance2 = Mockito.mock<Instance>(Instance::class.java)
        `when`(instance2.instanceId).thenReturn("i-new")
        `when`(instance2.privateIpAddress).thenReturn("privateIp2")
        `when`(instance2.publicIpAddress).thenReturn("publicIp2")

        val gatewayReservations = Arrays.asList(getReservation(instance1, instance2))

        `when`(describeInstancesResultGw!!.reservations).thenReturn(gatewayReservations)

        val ac = authenticatedContext()
        val statuses = awsMetadataCollector!!.collect(ac, null, vms)

        verify(amazonEC2Client, times(1)).createTags(any<CreateTagsRequest>(CreateTagsRequest::class.java))
        Assert.assertEquals(1, statuses.size.toLong())
        Assert.assertEquals("i-new", statuses[0].cloudVmInstanceStatus.cloudInstance.instanceId)
        Assert.assertEquals("privateIp2", statuses[0].metaData.privateIp)
        Assert.assertEquals("publicIp2", statuses[0].metaData.publicIp)
    }


    private fun getReservation(vararg instance: Instance): Reservation {
        val instances = Arrays.asList(*instance)
        val r = Reservation()
        r.setInstances(instances)
        return r
    }

    private fun authenticatedContext(): AuthenticatedContext {
        val location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"))
        val cloudContext = CloudContext(5L, "name", "platform", "owner", "variant", location)
        val cc = CloudCredential(1L, null, null, null)
        return AuthenticatedContext(cloudContext, cc)
    }


}
