package com.sequenceiq.it.cloudbreak

import java.util.ArrayList
import java.util.HashSet

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.ConfigFileApplicationContextInitializer
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.util.CollectionUtils
import org.springframework.util.StringUtils
import org.testng.ITestContext
import org.testng.annotations.AfterSuite
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Optional
import org.testng.annotations.Parameters

import com.sequenceiq.cloudbreak.api.endpoint.BlueprintEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.CredentialEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.NetworkEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.SecurityGroupEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.TemplateEndpoint
import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.it.IntegrationTestContext
import com.sequenceiq.it.SuiteContext
import com.sequenceiq.it.cloudbreak.config.ITProps
import com.sequenceiq.it.config.IntegrationTestConfiguration
import com.sequenceiq.it.util.CleanupService

@ContextConfiguration(classes = IntegrationTestConfiguration::class, initializers = ConfigFileApplicationContextInitializer::class)
class CloudbreakTestSuiteInitializer : AbstractTestNGSpringContextTests() {

    @Value("${integrationtest.cloudbreak.server}")
    private val defaultCloudbreakServer: String? = null
    @Value("${integrationtest.testsuite.cleanUpOnFailure}")
    private val cleanUpOnFailure: Boolean = false
    @Value("${integrationtest.defaultBlueprintName}")
    private val defaultBlueprintName: String? = null
    @Value("${integrationtest.testsuite.skipRemainingTestsAfterOneFailed}")
    private val skipRemainingSuiteTestsAfterOneFailed: Boolean = false
    @Value("${integrationtest.cleanup.cleanupBeforeStart}")
    private val cleanUpBeforeStart: Boolean = false
    @Value("${server.contextPath:/cb}")
    private val cbRootContextPath: String? = null

    @Inject
    private val itProps: ITProps? = null
    @Inject
    private val templateAdditionHelper: TemplateAdditionHelper? = null
    @Inject
    private val suiteContext: SuiteContext? = null
    @Inject
    private val cleanUpService: CleanupService? = null
    private var itContext: IntegrationTestContext? = null

    @BeforeSuite(dependsOnGroups = "suiteInit")
    @Throws(Exception::class)
    fun initContext(testContext: ITestContext) {
        // Workaround of https://jira.spring.io/browse/SPR-4072
        springTestContextBeforeTestClass()
        springTestContextPrepareTestInstance()

        itContext = suiteContext!!.getItContext(testContext.suite.name)
    }

    @BeforeSuite(dependsOnMethods = "initContext")
    @Parameters("cloudbreakServer", "cloudProvider", "credentialName", "instanceGroups", "hostGroups", "blueprintName", "stackName", "networkName", "securityGroupName")
    @Throws(Exception::class)
    fun initCloudbreakSuite(@Optional("") cloudbreakServer: String, @Optional("") cloudProvider: String, @Optional("") credentialName: String,
                            @Optional("") instanceGroups: String, @Optional("") hostGroups: String, @Optional("") blueprintName: String,
                            @Optional("") stackName: String, @Optional("") networkName: String, @Optional("") securityGroupName: String) {
        var cloudbreakServer = cloudbreakServer
        cloudbreakServer = if (StringUtils.hasLength(cloudbreakServer)) cloudbreakServer else defaultCloudbreakServer
        itContext!!.putContextParam(CloudbreakITContextConstants.SKIP_REMAINING_SUITETEST_AFTER_ONE_FAILED, skipRemainingSuiteTestsAfterOneFailed)
        itContext!!.putContextParam(CloudbreakITContextConstants.CLOUDBREAK_SERVER, cloudbreakServer)
        val identity = itContext!!.getContextParam(IntegrationTestContext.IDENTITY_URL)
        val user = itContext!!.getContextParam(IntegrationTestContext.AUTH_USER)
        val password = itContext!!.getContextParam(IntegrationTestContext.AUTH_PASSWORD)

        val cloudbreakClient = CloudbreakClient.CloudbreakClientBuilder(cloudbreakServer + cbRootContextPath!!, identity, "cloudbreak_shell").withCertificateValidation(false).withDebug(true).withCredential(user, password).build()
        itContext!!.putContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, cloudbreakClient)
        if (cleanUpBeforeStart) {
            cleanUpService!!.deleteTestStacksAndResources(cloudbreakClient)
        }
        putBlueprintToContextIfExist(
                itContext!!.getContextParam<CloudbreakClient>(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient::class.java).blueprintEndpoint(), blueprintName)
        putNetworkToContext(
                itContext!!.getContextParam<CloudbreakClient>(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient::class.java).networkEndpoint(), cloudProvider, networkName)
        putSecurityGroupToContext(itContext!!.getContextParam<CloudbreakClient>(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient::class.java).securityGroupEndpoint(),
                securityGroupName)
        putCredentialToContext(
                itContext!!.getContextParam<CloudbreakClient>(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient::class.java).credentialEndpoint(), cloudProvider,
                credentialName)
        putStackToContextIfExist(
                itContext!!.getContextParam<CloudbreakClient>(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient::class.java).stackEndpoint(), stackName)
        if (StringUtils.hasLength(instanceGroups)) {
            val instanceGroupStrings = templateAdditionHelper!!.parseCommaSeparatedRows(instanceGroups)
            itContext!!.putContextParam(CloudbreakITContextConstants.TEMPLATE_ID,
                    createInstanceGroups(itContext!!.getContextParam<CloudbreakClient>(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient::class.java).templateEndpoint(),
                            instanceGroupStrings))
        }
        if (StringUtils.hasLength(hostGroups)) {
            val hostGroupStrings = templateAdditionHelper!!.parseCommaSeparatedRows(hostGroups)
            itContext!!.putContextParam(CloudbreakITContextConstants.HOSTGROUP_ID, createHostGroups(hostGroupStrings))
        }
    }

    @Throws(Exception::class)
    private fun putBlueprintToContextIfExist(endpoint: BlueprintEndpoint, blueprintName: String) {
        var blueprintName = blueprintName
        endpoint.publics
        if (StringUtils.isEmpty(blueprintName)) {
            blueprintName = defaultBlueprintName
        }
        if (StringUtils.hasLength(blueprintName)) {
            val resourceId = endpoint.getPublic(blueprintName).id!!.toString()
            if (resourceId != null) {
                itContext!!.putContextParam(CloudbreakITContextConstants.BLUEPRINT_ID, resourceId)
            }
        }
    }

    @Throws(Exception::class)
    private fun putNetworkToContext(endpoint: NetworkEndpoint, cloudProvider: String, networkName: String) {
        var networkName = networkName
        endpoint.publics
        if (StringUtils.isEmpty(networkName)) {
            val defaultNetworkName = itProps!!.getDefaultNetwork(cloudProvider)
            networkName = defaultNetworkName
        }
        if (StringUtils.hasLength(networkName)) {
            val resourceId = endpoint.getPublic(networkName).id
            if (resourceId != null) {
                itContext!!.putContextParam(CloudbreakITContextConstants.NETWORK_ID, resourceId)
            }
        }
    }

    @Throws(Exception::class)
    private fun putSecurityGroupToContext(endpoint: SecurityGroupEndpoint, securityGroupName: String) {
        var securityGroupName = securityGroupName
        endpoint.publics
        if (StringUtils.isEmpty(securityGroupName)) {
            val defaultSecurityGroupName = itProps!!.defaultSecurityGroup
            securityGroupName = defaultSecurityGroupName
        }
        if (StringUtils.hasLength(securityGroupName)) {
            try {
                val resourceId = endpoint.getPublic(securityGroupName).id!!.toString()
                if (resourceId != null) {
                    itContext!!.putContextParam(CloudbreakITContextConstants.SECURITY_GROUP_ID, resourceId)
                }
            } catch (e: Exception) {
                LOG.warn("Could not set security group id", e)
            }

        }
    }

    @Throws(Exception::class)
    private fun putStackToContextIfExist(endpoint: StackEndpoint, stackName: String) {
        if (StringUtils.hasLength(stackName)) {
            val resourceId = endpoint.getPublic(stackName).id!!.toString()
            if (resourceId != null) {
                itContext!!.putContextParam(CloudbreakITContextConstants.STACK_ID, resourceId)
            }
        }
    }

    @Throws(Exception::class)
    private fun putCredentialToContext(endpoint: CredentialEndpoint, cloudProvider: String, credentialName: String) {
        var credentialName = credentialName
        if (StringUtils.isEmpty(credentialName)) {
            val defaultCredentialName = itProps!!.getCredentialName(cloudProvider)
            if ("__ignored__" != defaultCredentialName) {
                credentialName = defaultCredentialName
            }
        }
        if (StringUtils.hasLength(credentialName)) {
            val resourceId = endpoint.getPublic(credentialName).id!!.toString()
            if (resourceId != null) {
                itContext!!.putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, resourceId)
            }
        }
    }

    @Throws(Exception::class)
    private fun createInstanceGroups(endpoint: TemplateEndpoint, instanceGroupStrings: List<Array<String>>): List<InstanceGroup> {
        val instanceGroups = ArrayList<InstanceGroup>()
        for (instanceGroupStr in instanceGroupStrings) {
            val type = if (instanceGroupStr.size == WITH_TYPE_LENGTH) instanceGroupStr[WITH_TYPE_LENGTH - 1] else "CORE"
            instanceGroups.add(InstanceGroup(endpoint.getPublic(instanceGroupStr[0]).id!!.toString(), instanceGroupStr[1],
                    Integer.parseInt(instanceGroupStr[2]), type))
        }
        return instanceGroups
    }

    private fun createHostGroups(hostGroupStrings: List<Array<String>>): List<HostGroup> {
        val hostGroups = ArrayList<HostGroup>()
        for (hostGroupStr in hostGroupStrings) {
            hostGroups.add(HostGroup(hostGroupStr[0], hostGroupStr[1], Integer.valueOf(hostGroupStr[2])))
        }
        return hostGroups
    }

    @AfterSuite(alwaysRun = true)
    @Parameters("cleanUp")
    @Throws(Exception::class)
    fun cleanUp(@Optional("true") cleanUp: Boolean) {
        if (isCleanUpNeeded(cleanUp)) {
            val cloudbreakClient = itContext!!.getContextParam<CloudbreakClient>(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient::class.java)
            val stackId = itContext!!.getCleanUpParameter(CloudbreakITContextConstants.STACK_ID)
            cleanUpService!!.deleteStackAndWait(cloudbreakClient, stackId)
            val instanceGroups = itContext!!.getCleanUpParameter<List<Any>>(CloudbreakITContextConstants.TEMPLATE_ID, List<Any>::class.java)
            if (instanceGroups != null && !instanceGroups.isEmpty()) {
                val deletedTemplates = HashSet<String>()
                for (ig in instanceGroups) {
                    if (!deletedTemplates.contains(ig.templateId)) {
                        cleanUpService.deleteTemplate(cloudbreakClient, ig.templateId)
                        deletedTemplates.add(ig.templateId)
                    }
                }
            }
            val recipeIds = itContext!!.getContextParam<Set<Any>>(CloudbreakITContextConstants.RECIPE_ID, Set<Any>::class.java)
            if (recipeIds != null) {
                for (recipeId in recipeIds) {
                    cleanUpService.deleteRecipe(cloudbreakClient, recipeId)
                }
            }
            cleanUpService.deleteCredential(cloudbreakClient, itContext!!.getCleanUpParameter(CloudbreakITContextConstants.CREDENTIAL_ID))
            cleanUpService.deleteBlueprint(cloudbreakClient, itContext!!.getCleanUpParameter(CloudbreakITContextConstants.BLUEPRINT_ID))
            cleanUpService.deleteNetwork(cloudbreakClient, itContext!!.getCleanUpParameter(CloudbreakITContextConstants.NETWORK_ID))
            cleanUpService.deleteSecurityGroup(cloudbreakClient, itContext!!.getCleanUpParameter(CloudbreakITContextConstants.SECURITY_GROUP_ID))
        }
    }

    private fun isCleanUpNeeded(cleanUp: Boolean): Boolean {
        val noTestsFailed = CollectionUtils.isEmpty(itContext!!.getContextParam<List>(CloudbreakITContextConstants.FAILED_TESTS, List<Any>::class.java))
        return cleanUp && (cleanUpOnFailure || !cleanUpOnFailure && noTestsFailed)
    }

    companion object {
        private val WITH_TYPE_LENGTH = 4
        private val LOG = LoggerFactory.getLogger(CloudbreakTestSuiteInitializer::class.java)
    }
}
