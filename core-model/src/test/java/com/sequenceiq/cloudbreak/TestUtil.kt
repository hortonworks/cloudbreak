package com.sequenceiq.cloudbreak

import com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE
import com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS
import com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP
import com.sequenceiq.cloudbreak.common.type.CloudConstants.OPENSTACK

import java.net.URL
import java.util.ArrayList
import java.util.Arrays
import java.util.Date
import java.util.HashMap
import java.util.HashSet
import java.util.Random

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.fasterxml.jackson.core.JsonProcessingException
import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.api.model.InstanceStatus
import com.sequenceiq.cloudbreak.api.model.ExecutionType
import com.sequenceiq.cloudbreak.api.model.SssdProviderType
import com.sequenceiq.cloudbreak.api.model.SssdSchemaType
import com.sequenceiq.cloudbreak.api.model.SssdTlsReqcertType
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.common.type.CbUserRole
import com.sequenceiq.cloudbreak.common.type.ResourceStatus
import com.sequenceiq.cloudbreak.common.type.ResourceType
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Constraint
import com.sequenceiq.cloudbreak.domain.Credential
import com.sequenceiq.cloudbreak.domain.FailurePolicy
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Network
import com.sequenceiq.cloudbreak.domain.Orchestrator
import com.sequenceiq.cloudbreak.domain.RDSConfig
import com.sequenceiq.cloudbreak.domain.Recipe
import com.sequenceiq.cloudbreak.domain.Resource
import com.sequenceiq.cloudbreak.domain.SecurityGroup
import com.sequenceiq.cloudbreak.domain.SecurityRule
import com.sequenceiq.cloudbreak.domain.SssdConfig
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.domain.Template
import com.sequenceiq.cloudbreak.domain.json.Json

object TestUtil {

    val DUMMY_DESCRIPTION = "dummyDescription"
    val N1_HIGHCPU_16_INSTANCE = "n1-highcpu-16"

    private val LOGGER = LoggerFactory.getLogger(TestUtil::class.java)

    private val AZURE_PUB_KEY =
            "-----BEGIN CERTIFICATE-----\n"
    + "MIICsDCCAhmgAwIBAgIJAPtq+czPZYU/MA0GCSqGSIb3DQEBBQUAMEUxCzAJBgNV\n"
    + "BAYTAkFVMRMwEQYDVQQIEwpTb21lLVN0YXRlMSEwHwYDVQQKExhJbnRlcm5ldCBX\n"
    + "aWRnaXRzIFB0eSBMdGQwHhcNMTQwNTEzMDIxNDUwWhcNMTUwNTEzMDIxNDUwWjBF\n"
    + "MQswCQYDVQQGEwJBVTETMBEGA1UECBMKU29tZS1TdGF0ZTEhMB8GA1UEChMYSW50\n"
    + "ZXJuZXQgV2lkZ2l0cyBQdHkgTHRkMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKB\n"
    + "gQCvv6nBCp3wiqDVT0g1dEAJvfLiTU6oPVau9FCaNWrxJgkR697kuxMNhY4CpLXS\n"
    + "DgmSh/guI4iN5pmQtJ5RJsVBZRHWEu7k+GdvSFkNJ/7+i1t2DOjNtnOxGQ6TpjZg\n"
    + "lyDGNW2m2IY9iaaTzzwhowCcfMMwC+S0OzZ5AT3YE152XQIDAQABo4GnMIGkMB0G\n"
    + "A1UdDgQWBBR/lhZljxO+cPl9EQmfSb2sndrKFDB1BgNVHSMEbjBsgBR/lhZljxO+\n"
    + "cPl9EQmfSb2sndrKFKFJpEcwRTELMAkGA1UEBhMCQVUxEzARBgNVBAgTClNvbWUt\n"
    + "U3RhdGUxITAfBgNVBAoTGEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZIIJAPtq+czP\n"
    + "ZYU/MAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADgYEABYXu5HwJ8F9LyPrD\n"
    + "HkUQUM6HRoybllBZWf0uwrM5Mey/pYwhouR1PNd2/y6OXt5mjzxLG/53YvidfrEG\n"
    + "I5QW2HYwS3jZ2zlOLx5fj+wmeenxNrMxgP7XkbkVcBa76wdfZ1xBAr0ybXb13Gi2\n"
    + "TA0+meQcD7qPGKxxijqwU5Y1QTw=\n"
    + "-----END CERTIFICATE-----"
    private val DUMMY_NAME = "dummyName"

    fun getFilePath(clazz: Class<Any>, fileName: String): String {
        try {
            val resource = clazz.getResource(fileName)
            return resource.toURI().path
        } catch (ex: Exception) {
            LOGGER.error("{}: {}", ex.message, ex)
            return ""
        }

    }

    fun cbAdminUser(): CbUser {
        return CbUser("userid", "testuser", "testaccount", Arrays.asList(CbUserRole.ADMIN, CbUserRole.USER), "givenname", "familyname", Date())
    }

    fun cbUser(): CbUser {
        return CbUser("userid", "testuser", "testaccount", Arrays.asList(CbUserRole.USER), "givenname", "familyname", Date())
    }

    fun awsCredential(): Credential {
        val awsCredential = Credential()
        awsCredential.publicKey = AZURE_PUB_KEY
        awsCredential.isPublicInAccount = false
        awsCredential.isArchived = false
        awsCredential.setCloudPlatform(AWS)
        awsCredential.description = DUMMY_DESCRIPTION
        awsCredential.id = 1L
        awsCredential.loginUserName = "cb"
        awsCredential.name = DUMMY_NAME
        return awsCredential
    }

    fun gcpCredential(): Credential {
        val credential = Credential()
        credential.publicKey = AZURE_PUB_KEY
        credential.id = 1L
        credential.name = DUMMY_NAME
        credential.setCloudPlatform(GCP)
        credential.loginUserName = "cb"
        credential.isPublicInAccount = true
        credential.description = DUMMY_DESCRIPTION
        return credential
    }

    fun setEphemeral(stack: Stack): Stack {
        if (stack.cloudPlatform() == AWS) {
            for (instanceGroup in stack.instanceGroups) {
                instanceGroup.template.volumeType = "ephemeral"
            }
        }
        return stack
    }

    @JvmOverloads fun stack(stackStatus: Status = AVAILABLE, credential: Credential = gcpCredential()): Stack {
        val stack = Stack()
        stack.status = stackStatus
        stack.credential = credential
        stack.name = "simplestack"
        stack.owner = "userid"
        stack.account = "account"
        stack.id = 1L
        stack.instanceGroups = generateGcpInstanceGroups(3)
        stack.securityGroup = securityGroup(1L)
        stack.statusReason = "statusReason"
        stack.region = "region"
        stack.created = 123L
        stack.setCloudPlatform(credential.cloudPlatform())
        stack.orchestrator = orchestrator()
        stack.relocateDocker = true
        when (credential.cloudPlatform()) {
            AWS -> stack.instanceGroups = generateAwsInstanceGroups(3)
            GCP -> stack.instanceGroups = generateGcpInstanceGroups(3)
            OPENSTACK -> stack.instanceGroups = generateOpenStackInstanceGroups(3)
            else -> {
            }
        }
        return stack
    }

    fun orchestrator(): Orchestrator {
        val orchestrator = Orchestrator()
        orchestrator.type = "DUMMY"
        orchestrator.apiEndpoint = "endpoint"
        try {
            orchestrator.attributes = Json("{\"test\": \"test\"}")
        } catch (e: JsonProcessingException) {
            orchestrator.attributes = null
        }

        orchestrator.id = 1L
        return orchestrator
    }

    private fun securityGroup(id: Long): SecurityGroup {
        val sg = SecurityGroup()
        sg.id = id
        sg.name = "security-group"
        sg.isPublicInAccount = true
        sg.securityRules = HashSet<SecurityRule>()
        sg.status = ResourceStatus.DEFAULT
        return sg
    }

    fun generateAwsInstanceGroups(count: Int): Set<InstanceGroup> {
        val instanceGroups = HashSet<InstanceGroup>()
        instanceGroups.add(instanceGroup(1L, InstanceGroupType.GATEWAY, awsTemplate(1L)))
        for (i in 0..count - 1 - 1) {
            instanceGroups.add(instanceGroup(1L, InstanceGroupType.CORE, awsTemplate(1L)))
        }
        return instanceGroups
    }

    fun generateOpenStackInstanceGroups(count: Int): Set<InstanceGroup> {
        val instanceGroups = HashSet<InstanceGroup>()
        instanceGroups.add(instanceGroup(1L, InstanceGroupType.GATEWAY, gcpTemplate(1L)))
        for (i in 0..count - 1 - 1) {
            instanceGroups.add(instanceGroup(1L, InstanceGroupType.CORE, gcpTemplate(1L)))
        }
        return instanceGroups
    }

    fun generateGcpInstanceGroups(count: Int): Set<InstanceGroup> {
        val instanceGroups = HashSet<InstanceGroup>()
        instanceGroups.add(instanceGroup(1L, InstanceGroupType.GATEWAY, openstackTemplate(1L)))
        for (i in 0..count - 1 - 1) {
            instanceGroups.add(instanceGroup(1L, InstanceGroupType.CORE, openstackTemplate(1L)))
        }
        return instanceGroups
    }

    @JvmOverloads fun instanceGroup(id: Long?, instanceGroupType: InstanceGroupType, template: Template, nodeCount: Int = 1): InstanceGroup {
        val instanceGroup = InstanceGroup()
        instanceGroup.id = id
        instanceGroup.nodeCount = nodeCount
        instanceGroup.groupName = "is" + id!!
        instanceGroup.instanceGroupType = instanceGroupType
        instanceGroup.template = template
        instanceGroup.instanceMetaData = generateInstanceMetaDatas(1, id, instanceGroup)
        return instanceGroup
    }

    @JvmOverloads fun network(subnet: String = "10.0.0.1/16"): Network {
        val network = Network()
        network.subnetCIDR = subnet
        //        network.setAddressPrefixCIDR(DUMMY_ADDRESS_PREFIX_CIDR);
        network.id = 1L
        network.name = DUMMY_NAME
        return network
    }

    fun instanceMetaData(id: Long?, instanceStatus: InstanceStatus, ambariServer: Boolean, instanceGroup: InstanceGroup): InstanceMetaData {
        val random = Random()
        val instanceMetaData = InstanceMetaData()
        instanceMetaData.instanceStatus = instanceStatus
        instanceMetaData.ambariServer = ambariServer
        instanceMetaData.consulServer = true
        instanceMetaData.sshPort = 22
        instanceMetaData.discoveryFQDN = "test" + id!!
        instanceMetaData.instanceId = "test" + id
        instanceMetaData.privateIp = "1.1.1." + (id + Math.abs(random.nextInt(255)))
        instanceMetaData.publicIp = "2.2.2." + (id + Math.abs(random.nextInt(255)))
        instanceMetaData.id = id
        instanceMetaData.instanceGroup = instanceGroup
        instanceMetaData.startDate = Date().time
        return instanceMetaData
    }

    fun generateInstanceMetaDatas(count: Int, instanceGroupId: Long?, instanceGroup: InstanceGroup): Set<InstanceMetaData> {
        val instanceMetaDatas = HashSet<InstanceMetaData>()
        for (i in 0..count - 1) {
            instanceMetaDatas.add(instanceMetaData(java.lang.Long.valueOf(i + instanceGroupId!!), InstanceStatus.REGISTERED,
                    if (instanceGroup.instanceGroupType == InstanceGroupType.GATEWAY) true else false, instanceGroup))
        }
        return instanceMetaDatas
    }

    fun awsTemplate(id: Long?): Template {
        val awsTemplate = Template()
        awsTemplate.instanceType = "c3.2xlarge"
        awsTemplate.id = id
        awsTemplate.setCloudPlatform(AWS)
        awsTemplate.volumeCount = 1
        awsTemplate.volumeSize = 100
        awsTemplate.volumeType = "standard"
        awsTemplate.id = 1L
        awsTemplate.name = DUMMY_NAME
        awsTemplate.description = DUMMY_DESCRIPTION
        awsTemplate.isPublicInAccount = true
        return awsTemplate
    }

    fun openstackTemplate(id: Long?): Template {
        val openStackTemplate = Template()
        openStackTemplate.instanceType = "Big"
        openStackTemplate.setCloudPlatform(OPENSTACK)
        openStackTemplate.id = id
        openStackTemplate.volumeCount = 1
        openStackTemplate.volumeSize = 100
        openStackTemplate.name = DUMMY_NAME
        openStackTemplate.isPublicInAccount = true
        openStackTemplate.status = ResourceStatus.DEFAULT
        openStackTemplate.description = DUMMY_DESCRIPTION
        return openStackTemplate
    }

    fun gcpTemplate(id: Long?): Template {
        val gcpTemplate = Template()
        gcpTemplate.instanceType = N1_HIGHCPU_16_INSTANCE
        gcpTemplate.id = id
        gcpTemplate.setCloudPlatform(GCP)
        gcpTemplate.volumeCount = 1
        gcpTemplate.volumeSize = 100
        gcpTemplate.description = DUMMY_DESCRIPTION
        gcpTemplate.isPublicInAccount = true
        gcpTemplate.status = ResourceStatus.DEFAULT
        gcpTemplate.name = DUMMY_NAME
        return gcpTemplate
    }

    fun generateCluster(count: Int): List<Cluster> {
        val clusters = ArrayList<Cluster>()
        for (i in 0..count - 1) {
            clusters.add(cluster(TestUtil.blueprint(), stack(AVAILABLE, gcpCredential()), i.toLong()))
        }
        return clusters
    }

    fun cluster(blueprint: Blueprint, stack: Stack, id: Long?): Cluster {
        return cluster(blueprint, null, stack, id)
    }

    fun cluster(blueprint: Blueprint, sssdConfig: SssdConfig?, stack: Stack, id: Long?): Cluster {
        val cluster = Cluster()
        cluster.ambariIp = "50.51.52.100"
        cluster.stack = stack
        cluster.id = id
        cluster.name = "dummyCluster"
        cluster.ambariIp = "10.0.0.1"
        cluster.blueprint = blueprint
        cluster.upSince = Date().time
        cluster.status = AVAILABLE
        cluster.statusReason = "statusReason"
        cluster.userName = "admin"
        cluster.password = "admin"
        cluster.sssdConfig = sssdConfig
        cluster.enableShipyard = true
        val ambariStackDetails = AmbariStackDetails()
        cluster.ambariStackDetails = ambariStackDetails
        val rdsConfig = RDSConfig()
        cluster.rdsConfig = rdsConfig
        cluster.hostGroups = hostGroups(cluster)
        return cluster
    }

    fun hostGroup(): HostGroup {
        val hostGroup = HostGroup()
        hostGroup.id = 1L
        hostGroup.name = DUMMY_NAME
        hostGroup.recipes = TestUtil.recipes(1)
        hostGroup.hostMetadata = TestUtil.hostMetadata(hostGroup, 1)
        val instanceGroup = TestUtil.instanceGroup(1L, InstanceGroupType.CORE, TestUtil.gcpTemplate(1L))
        val constraint = Constraint()
        constraint.instanceGroup = instanceGroup
        constraint.hostCount = instanceGroup.nodeCount
        hostGroup.constraint = constraint
        hostGroup.cluster = TestUtil.cluster(TestUtil.blueprint(), TestUtil.stack(), 1L)
        return hostGroup
    }

    fun hostGroups(cluster: Cluster): Set<HostGroup> {
        val hostGroups = HashSet<HostGroup>()
        val hg = HostGroup()
        hg.cluster = cluster
        hg.id = 1L
        hg.name = "slave_1"
        hostGroups.add(hg)
        return hostGroups
    }

    fun hostMetadata(hostGroup: HostGroup, count: Int): Set<HostMetadata> {
        val hostMetadataSet = HashSet<HostMetadata>()
        for (i in 1..count) {
            val hostMetadata = HostMetadata()
            hostMetadata.hostName = "hostname-" + (i + 1)
            hostMetadata.hostGroup = hostGroup
            hostMetadataSet.add(hostMetadata)
        }
        return hostMetadataSet
    }

    fun recipes(count: Int): MutableSet<Recipe> {
        val recipes = HashSet<Recipe>()
        for (i in 0..count - 1) {
            val recipe = Recipe()
            recipe.description = "description"
            recipe.id = (i + 1).toLong()
            recipe.name = "recipe-" + (i + 1)
            recipe.timeout = 100
            recipe.isPublicInAccount = true
            recipe.plugins = createRecipePlugins()
            recipe.keyValues = HashMap<String, String>()
            recipes.add(recipe)
        }
        return recipes
    }

    fun sssdConfigs(count: Int): Set<SssdConfig> {
        val configs = HashSet<SssdConfig>()
        for (i in 0..count - 1) {
            val config = SssdConfig()
            config.id = i.toLong()
            config.name = "config-" + (i + 1)
            config.description = "description"
            config.providerType = SssdProviderType.LDAP
            config.url = "ldap://ldap.domain"
            config.schema = SssdSchemaType.RFC2307
            config.baseSearch = "dc=domain"
            config.tlsReqcert = SssdTlsReqcertType.NEVER
            config.adServer = "ad.domain"
            config.kerberosServer = "kerberos.domain"
            config.kerberosRealm = "KERBEROS_DOMAIN"
            config.configuration = ""
            config.isPublicInAccount = true
            configs.add(config)
        }
        return configs
    }

    fun blueprint(): Blueprint {
        val blueprint = Blueprint()
        blueprint.id = 1L
        blueprint.blueprintText = "{\"host_groups\":[{\"name\":\"slave_1\",\"components\":[{\"name\":\"DATANODE\"}]}]}"
        blueprint.name = "multi-node-yarn"
        blueprint.blueprintName = "multi-node-yarn"
        return blueprint
    }

    fun generateAzureCloudbreakUsages(count: Int): List<CloudbreakUsage> {
        val cloudbreakUsages = ArrayList<CloudbreakUsage>()
        for (i in 0..count - 1) {
            cloudbreakUsages.add(gcpCloudbreakUsage(java.lang.Long.valueOf(i.toLong())))
        }
        return cloudbreakUsages
    }

    fun gcpCloudbreakUsage(id: Long?): CloudbreakUsage {
        val cloudbreakUsage = CloudbreakUsage()
        cloudbreakUsage.id = id
        cloudbreakUsage.instanceGroup = "master"
        cloudbreakUsage.account = "account"
        cloudbreakUsage.costs = 2.0
        cloudbreakUsage.day = Date()
        cloudbreakUsage.instanceHours = 1L
        cloudbreakUsage.instanceType = "xlarge"
        cloudbreakUsage.owner = "owner"
        cloudbreakUsage.provider = GCP
        cloudbreakUsage.region = "Central US"
        cloudbreakUsage.stackName = "usagestack"
        cloudbreakUsage.stackId = 1L
        return cloudbreakUsage
    }

    fun generateGcpCloudbreakEvents(count: Int): List<CloudbreakEvent> {
        val cloudbreakEvents = ArrayList<CloudbreakEvent>()
        for (i in 0..count - 1) {
            cloudbreakEvents.add(gcpCloudbreakEvent(java.lang.Long.valueOf(i.toLong())))
        }
        return cloudbreakEvents
    }

    fun gcpCloudbreakEvent(id: Long?): CloudbreakEvent {
        val cloudbreakEvent = CloudbreakEvent()
        cloudbreakEvent.id = id
        cloudbreakEvent.instanceGroup = "master"
        cloudbreakEvent.account = "account"
        cloudbreakEvent.owner = "owner"
        cloudbreakEvent.region = "us"
        cloudbreakEvent.stackName = "usagestack"
        cloudbreakEvent.stackId = 1L
        cloudbreakEvent.eventTimestamp = Date()
        cloudbreakEvent.eventMessage = "message"
        cloudbreakEvent.eventType = "eventType"
        cloudbreakEvent.cloud = GCP
        cloudbreakEvent.blueprintName = "blueprintName"
        cloudbreakEvent.blueprintId = 1L
        cloudbreakEvent.stackStatus = AVAILABLE
        cloudbreakEvent.nodeCount = 1
        cloudbreakEvent.clusterStatus = AVAILABLE
        cloudbreakEvent.clusterId = 1L
        cloudbreakEvent.clusterName = "test"
        return cloudbreakEvent
    }

    private fun createRecipePlugins(): Map<String, ExecutionType> {
        val plugin = HashMap<String, ExecutionType>()
        plugin.put("all-node-plugin", ExecutionType.ALL_NODES)
        plugin.put("one-node-plugin", ExecutionType.ONE_NODE)
        return plugin
    }

    fun ambariStackDetails(): AmbariStackDetails {
        val ambariStackDetails = AmbariStackDetails()
        ambariStackDetails.os = "dummyOs"
        ambariStackDetails.stack = "dummyStack"
        ambariStackDetails.stackBaseURL = "dummyStackBaseUrl"
        ambariStackDetails.stackRepoId = "dummyStackRepoId"
        ambariStackDetails.utilsBaseURL = "dummyUtilsBaseUrl"
        ambariStackDetails.utilsRepoId = "dummyUtilsRepoId"
        ambariStackDetails.isVerify = true
        ambariStackDetails.version = "0.1.0"
        return ambariStackDetails
    }

    fun failurePolicy(): FailurePolicy {
        val failurePolicy = FailurePolicy()
        failurePolicy.id = 1L
        failurePolicy.threshold = 10L
        failurePolicy.adjustmentType = AdjustmentType.BEST_EFFORT
        return failurePolicy
    }

    fun securityGroup(securityRules: Set<SecurityRule>): SecurityGroup {
        val securityGroup = SecurityGroup()
        securityGroup.isPublicInAccount = true
        securityGroup.description = DUMMY_DESCRIPTION
        securityGroup.id = 1L
        securityGroup.name = DUMMY_NAME
        securityGroup.status = ResourceStatus.DEFAULT
        securityGroup.securityRules = securityRules
        return securityGroup
    }

    fun generateGcpInstanceGroupsByNodeCount(vararg count: Int): Set<InstanceGroup> {
        val instanceGroups = HashSet<InstanceGroup>()
        instanceGroups.add(instanceGroup(0L, InstanceGroupType.GATEWAY, gcpTemplate(1L), count[0]))
        for (i in 1..count.size - 1) {
            instanceGroups.add(instanceGroup(java.lang.Long.valueOf(i.toLong()), InstanceGroupType.CORE, gcpTemplate(1L), count[i]))
        }
        return instanceGroups
    }

    fun generateGcpResources(count: Int): List<Resource> {
        val resources = ArrayList<Resource>()
        for (i in 0..count - 1) {
            resources.add(gcpResource(java.lang.Long.valueOf(i.toLong()), "master"))
        }
        return resources
    }

    fun gcpResource(id: Long?, instanceGroup: String): Resource {
        val resource = Resource()
        resource.id = id
        resource.stack = stack()
        resource.instanceGroup = instanceGroup
        resource.resourceName = "testResource"
        resource.resourceType = ResourceType.GCP_INSTANCE
        return resource
    }
}
