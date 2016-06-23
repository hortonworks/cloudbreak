package com.sequenceiq.it.cloudbreak.mock

import com.sequenceiq.it.spark.ITResponse.CONSUL_API_ROOT
import com.sequenceiq.it.spark.ITResponse.MOCK_ROOT
import com.sequenceiq.it.spark.ITResponse.SALT_API_ROOT
import com.sequenceiq.it.spark.ITResponse.SALT_BOOT_ROOT
import spark.Spark.get
import spark.Spark.port
import spark.Spark.post

import java.util.ArrayList
import java.util.HashMap

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ResourceLoader
import org.springframework.http.HttpStatus
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Optional
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.api.model.FailurePolicyJson
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.api.model.OnFailureAction
import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest
import com.sequenceiq.cloudbreak.api.model.StackRequest
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses
import com.sequenceiq.it.IntegrationTestContext
import com.sequenceiq.it.cloudbreak.AbstractMockIntegrationTest
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants
import com.sequenceiq.it.cloudbreak.CloudbreakUtil
import com.sequenceiq.it.cloudbreak.InstanceGroup
import com.sequenceiq.it.spark.consul.ConsulMemberResponse
import com.sequenceiq.it.spark.salt.SaltApiRunPostResponse
import com.sequenceiq.it.spark.spi.CloudMetaDataStatuses
import com.sequenceiq.it.util.ServerAddressGenerator
import com.sequenceiq.it.verification.Verification

class MockStackCreationWithSaltSuccessTest : AbstractMockIntegrationTest() {

    @Value("${mock.server.address:localhost}")
    private val mockServerAddress: String? = null

    @Inject
    private val resourceLoader: ResourceLoader? = null

    @BeforeMethod
    fun setContextParams() {
        val itContext = itContext
        Assert.assertNotNull(itContext.getContextParam<List<Any>>(CloudbreakITContextConstants.TEMPLATE_ID, List<Any>::class.java), "Template id is mandatory.")
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.CREDENTIAL_ID), "Credential id is mandatory.")
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.NETWORK_ID), "Network id is mandatory.")
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.SECURITY_GROUP_ID), "Security group id is mandatory.")
    }

    @Test
    @Parameters("stackName", "region", "onFailureAction", "threshold", "adjustmentType", "variant", "availabilityZone", "persistentStorage", "orchestrator", "mockPort", "sshPort")
    @Throws(Exception::class)
    fun testStackCreation(@Optional("testing1") stackName: String, @Optional("europe-west1") region: String,
                          @Optional("DO_NOTHING") onFailureAction: String, @Optional("4") threshold: Long?, @Optional("EXACT") adjustmentType: String,
                          @Optional("") variant: String, @Optional availabilityZone: String, @Optional persistentStorage: String?, @Optional("SWARM") orchestrator: String,
                          @Optional("443") mockPort: Int, @Optional("2020") sshPort: Int) {
        // GIVEN
        val itContext = itContext
        val instanceGroups = itContext.getContextParam<List<Any>>(CloudbreakITContextConstants.TEMPLATE_ID, List<Any>::class.java)
        val igMap = ArrayList<InstanceGroupJson>()
        for (ig in instanceGroups) {
            val instanceGroupJson = InstanceGroupJson()
            instanceGroupJson.group = ig.name
            instanceGroupJson.nodeCount = ig.nodeCount
            instanceGroupJson.templateId = java.lang.Long.valueOf(ig.templateId)
            instanceGroupJson.type = InstanceGroupType.valueOf(ig.type)
            igMap.add(instanceGroupJson)
        }
        val credentialId = itContext.getContextParam(CloudbreakITContextConstants.CREDENTIAL_ID)
        val networkId = itContext.getContextParam(CloudbreakITContextConstants.NETWORK_ID)
        val securityGroupId = itContext.getContextParam(CloudbreakITContextConstants.SECURITY_GROUP_ID)
        val stackRequest = StackRequest()
        stackRequest.name = stackName
        stackRequest.credentialId = java.lang.Long.valueOf(credentialId)
        stackRequest.region = region
        stackRequest.onFailureAction = OnFailureAction.valueOf(onFailureAction)
        val failurePolicyJson = FailurePolicyJson()
        failurePolicyJson.adjustmentType = AdjustmentType.valueOf(adjustmentType)
        failurePolicyJson.setThreshold(threshold!!)
        stackRequest.failurePolicy = failurePolicyJson
        stackRequest.networkId = java.lang.Long.valueOf(networkId)
        stackRequest.securityGroupId = java.lang.Long.valueOf(securityGroupId)
        stackRequest.platformVariant = variant
        stackRequest.availabilityZone = availabilityZone
        stackRequest.instanceGroups = igMap

        val orchestratorRequest = OrchestratorRequest()
        orchestratorRequest.type = orchestrator
        stackRequest.orchestrator = orchestratorRequest

        val map = HashMap<String, String>()
        if (persistentStorage != null && !persistentStorage.isEmpty()) {
            map.put("persistentStorage", persistentStorage)
        }
        stackRequest.parameters = map
        val numberOfServers = getServerCount(instanceGroups)

        port(mockPort)
        addSPIEndpoints(sshPort)
        addMockEndpoints(numberOfServers)
        initSpark()

        // WHEN
        val stackId = cloudbreakClient.stackEndpoint().postPrivate(stackRequest).id!!.toString()
        // THEN
        Assert.assertNotNull(stackId)
        itContext.putCleanUpParam(CloudbreakITContextConstants.STACK_ID, stackId)
        CloudbreakUtil.waitAndCheckStackStatus(cloudbreakClient, stackId, "AVAILABLE")
        itContext.putContextParam(CloudbreakITContextConstants.STACK_ID, stackId)

        verifyCalls(numberOfServers)
    }

    private fun addSPIEndpoints(sshPort: Int) {
        post(MOCK_ROOT + "/cloud_metadata_statuses", CloudMetaDataStatuses(mockServerAddress, sshPort), ResponseTransformer { gson().toJson(it) })
    }

    private fun addMockEndpoints(numberOfServers: Int) {
        get(SALT_BOOT_ROOT + "/health", { request, response ->
            val genericResponse = GenericResponse()
            genericResponse.statusCode = HttpStatus.OK.value()
            genericResponse
        }, ResponseTransformer { gson().toJson(it) })
        post(SALT_BOOT_ROOT + "/salt/server/pillar", { request, response ->
            val genericResponse = GenericResponse()
            genericResponse.statusCode = HttpStatus.OK.value()
            genericResponse
        }, ResponseTransformer { gson().toJson(it) })
        post(SALT_BOOT_ROOT + "/salt/action/distribute", { request, response ->
            val genericResponses = GenericResponses()
            genericResponses.responses = ArrayList<GenericResponse>()
            genericResponses
        }, ResponseTransformer { gson().toJson(it) })
        post(SALT_BOOT_ROOT + "/hostname/distribute", { request, response ->
            val genericResponses = GenericResponses()
            val responses = ArrayList<GenericResponse>()
            ServerAddressGenerator(numberOfServers).iterateOver { address ->
                val genericResponse = GenericResponse()
                genericResponse.address = address
                genericResponse.status = "host-" + address.replace(".", "-")
                responses.add(genericResponse)
            }
            genericResponses.responses = responses
            genericResponses
        }, ResponseTransformer { gson().toJson(it) })
        post(SALT_BOOT_ROOT + "/file") { request, response ->
            response.status(200)
            response
        }
        post(SALT_API_ROOT + "/run", SaltApiRunPostResponse(numberOfServers))
        get(CONSUL_API_ROOT + "/agent/members", "application/json", ConsulMemberResponse(numberOfServers), ResponseTransformer { gson().toJson(it) })
    }

    private fun verifyCalls(numberOfServers: Int) {
        verify(SALT_BOOT_ROOT + "/health", "GET").exactTimes(1).verify()
        verify(SALT_BOOT_ROOT + "/file", "POST").exactTimes(1).verify()
        val distributeVerification = verify(SALT_BOOT_ROOT + "/salt/action/distribute", "POST").exactTimes(1)
        ServerAddressGenerator(numberOfServers).iterateOver { address -> distributeVerification.bodyContains("address\":\"$address") }
        distributeVerification.verify()
    }

    private fun getServerCount(instanceGroups: List<InstanceGroup>): Int {
        var numberOfServers = 0
        for (instanceGroup in instanceGroups) {
            numberOfServers += instanceGroup.nodeCount
        }
        return numberOfServers
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(MockStackCreationWithSaltSuccessTest::class.java)
    }

}
