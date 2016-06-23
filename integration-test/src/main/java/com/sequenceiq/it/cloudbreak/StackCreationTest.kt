package com.sequenceiq.it.cloudbreak

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

class StackCreationTest : AbstractCloudbreakIntegrationTest() {

    @BeforeMethod
    fun setContextParams() {
        val itContext = itContext
        Assert.assertNotNull(itContext.getContextParam<List<Any>>(CloudbreakITContextConstants.TEMPLATE_ID, List<Any>::class.java), "Template id is mandatory.")
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.CREDENTIAL_ID), "Credential id is mandatory.")
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.NETWORK_ID), "Network id is mandatory.")
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.SECURITY_GROUP_ID), "Security group id is mandatory.")
    }

    @Test
    @Parameters("stackName", "region", "onFailureAction", "threshold", "adjustmentType", "variant", "availabilityZone", "persistentStorage", "orchestrator")
    @Throws(Exception::class)
    fun testStackCreation(@Optional("testing1") stackName: String, @Optional("europe-west1") region: String,
                          @Optional("DO_NOTHING") onFailureAction: String, @Optional("4") threshold: Long?, @Optional("EXACT") adjustmentType: String,
                          @Optional("") variant: String, @Optional availabilityZone: String, @Optional persistentStorage: String?, @Optional("SALT") orchestrator: String) {
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

        // WHEN
        val stackId = cloudbreakClient.stackEndpoint().postPrivate(stackRequest).id!!.toString()
        // THEN
        Assert.assertNotNull(stackId)
        itContext.putCleanUpParam(CloudbreakITContextConstants.STACK_ID, stackId)
        CloudbreakUtil.waitAndCheckStackStatus(cloudbreakClient, stackId, "AVAILABLE")
        itContext.putContextParam(CloudbreakITContextConstants.STACK_ID, stackId)
    }
}
