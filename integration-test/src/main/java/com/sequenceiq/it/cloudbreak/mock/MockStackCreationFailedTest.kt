package com.sequenceiq.it.cloudbreak.mock

import com.sequenceiq.it.spark.ITResponse.CONSUL_API_ROOT
import com.sequenceiq.it.spark.ITResponse.DOCKER_API_ROOT
import com.sequenceiq.it.spark.ITResponse.SWARM_API_ROOT
import spark.Spark.get
import spark.Spark.port
import spark.Spark.post

import java.util.ArrayList
import java.util.HashMap

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
import com.sequenceiq.it.IntegrationTestContext
import com.sequenceiq.it.cloudbreak.AbstractMockIntegrationTest
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants
import com.sequenceiq.it.cloudbreak.CloudbreakUtil
import com.sequenceiq.it.cloudbreak.InstanceGroup
import com.sequenceiq.it.spark.consul.ConsulMemberResponse
import com.sequenceiq.it.spark.docker.model.Info
import com.sequenceiq.it.spark.docker.model.InspectContainerResponse

class MockStackCreationFailedTest : AbstractMockIntegrationTest() {

    @BeforeMethod
    fun setContextParams() {
        val itContext = itContext
        Assert.assertNotNull(itContext.getContextParam<List<Any>>(CloudbreakITContextConstants.TEMPLATE_ID, List<Any>::class.java), "Template id is mandatory.")
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.CREDENTIAL_ID), "Credential id is mandatory.")
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.NETWORK_ID), "Network id is mandatory.")
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.SECURITY_GROUP_ID), "Security group id is mandatory.")
    }

    @Test
    @Parameters("stackName", "region", "onFailureAction", "threshold", "adjustmentType", "variant", "availabilityZone", "persistentStorage", "orchestrator", "mockPort")
    @Throws(Exception::class)
    fun testStackCreation(@Optional("testing1") stackName: String, @Optional("europe-west1") region: String,
                          @Optional("DO_NOTHING") onFailureAction: String, @Optional("4") threshold: Long?, @Optional("EXACT") adjustmentType: String,
                          @Optional("") variant: String, @Optional availabilityZone: String, @Optional persistentStorage: String?, @Optional("SWARM") orchestrator: String,
                          @Optional("false") useMockServer: Boolean, @Optional("443") mockPort: Int) {
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

        val numberOfServers = getNumberOfServers(instanceGroups)

        port(mockPort)
        addMockEndpoints(numberOfServers)
        initSpark()

        // WHEN
        val stackId = cloudbreakClient.stackEndpoint().postPrivate(stackRequest).id!!.toString()
        // THEN
        Assert.assertNotNull(stackId)
        itContext.putCleanUpParam(CloudbreakITContextConstants.STACK_ID, stackId)
        CloudbreakUtil.waitAndCheckStackStatus(cloudbreakClient, stackId, "CREATE_FAILED")
        itContext.putContextParam(CloudbreakITContextConstants.STACK_ID, stackId)
    }

    private fun addMockEndpoints(numberOfServers: Int) {
        get(DOCKER_API_ROOT + "/info") { req, res -> "" }
        get(DOCKER_API_ROOT + "/containers/:container/json", { req, res -> InspectContainerResponse("id") }, ResponseTransformer { gson().toJson(it) })
        post(DOCKER_API_ROOT + "/containers/:container/start") { req, res -> "" }
        get(SWARM_API_ROOT + "/info", { req, res -> Info(numberOfServers) }, ResponseTransformer { gson().toJson(it) })
        oneConsulMemberFailedToStart(numberOfServers)
    }

    private fun oneConsulMemberFailedToStart(numberOfServers: Int) {
        get(CONSUL_API_ROOT + "/agent/members", ConsulMemberResponse(numberOfServers), ResponseTransformer { gson().toJson(it) })
    }

    private fun getNumberOfServers(instanceGroups: List<InstanceGroup>): Int {
        var numberOfServers = 0
        for (instanceGroup in instanceGroups) {
            numberOfServers += instanceGroup.nodeCount
        }
        return numberOfServers
    }
}
