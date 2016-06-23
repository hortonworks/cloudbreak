package com.sequenceiq.it.cloudbreak

import java.util.Arrays
import java.util.Collections
import java.util.HashSet

import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Optional
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.sequenceiq.cloudbreak.api.endpoint.ClusterEndpoint
import com.sequenceiq.cloudbreak.api.model.ClusterRequest
import com.sequenceiq.cloudbreak.api.model.ConstraintJson
import com.sequenceiq.cloudbreak.api.model.HostGroupJson
import com.sequenceiq.it.IntegrationTestContext


class ClusterCreationTest : AbstractCloudbreakIntegrationTest() {

    @BeforeMethod
    fun setContextParameters() {
        val itContext = itContext
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.BLUEPRINT_ID), "Blueprint id is mandatory.")
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.")
    }

    @Test
    @Parameters("clusterName", "ambariPort", "ambariUser", "ambariPassword", "emailNeeded", "enableSecurity", "kerberosMasterKey", "kerberosAdmin", "kerberosPassword", "runRecipesOnHosts", "checkAmbari")
    @Throws(Exception::class)
    fun testClusterCreation(@Optional("it-cluster") clusterName: String, @Optional("8080") ambariPort: String, @Optional("admin") ambariUser: String,
                            @Optional("admin123!@#") ambariPassword: String, @Optional("false") emailNeeded: Boolean,
                            @Optional("false") enableSecurity: Boolean, @Optional kerberosMasterKey: String, @Optional kerberosAdmin: String, @Optional kerberosPassword: String,
                            @Optional("") runRecipesOnHosts: String, @Optional("true") checkAmbari: Boolean) {
        // GIVEN
        val itContext = itContext
        val stackIdStr = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID)
        val stackId = Integer.valueOf(stackIdStr)
        val blueprintId = Integer.valueOf(itContext.getContextParam(CloudbreakITContextConstants.BLUEPRINT_ID))
        val hostgroups = itContext.getContextParam<List<Any>>(CloudbreakITContextConstants.HOSTGROUP_ID, List<Any>::class.java)
        val hostGroupJsons1 = convertHostGroups(hostgroups, runRecipesOnHosts)
        itContext.putContextParam(CloudbreakITContextConstants.AMBARI_USER_ID, ambariUser)
        itContext.putContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID, ambariPassword)
        // WHEN
        // TODO email needed
        val clusterRequest = ClusterRequest()
        clusterRequest.name = clusterName
        clusterRequest.description = "Cluster for integration test"
        clusterRequest.kerberosAdmin = kerberosAdmin
        clusterRequest.kerberosPassword = kerberosPassword
        clusterRequest.kerberosMasterKey = kerberosMasterKey
        clusterRequest.enableSecurity = enableSecurity
        clusterRequest.password = ambariPassword
        clusterRequest.userName = ambariUser
        clusterRequest.blueprintId = java.lang.Long.valueOf(blueprintId!!.toLong())
        clusterRequest.hostGroups = hostGroupJsons1

        val clusterEndpoint = cloudbreakClient.clusterEndpoint()
        CloudbreakUtil.checkResponse("ClusterCreation", clusterEndpoint.post(java.lang.Long.valueOf(stackId!!.toLong()), clusterRequest))
        // THEN
        CloudbreakUtil.waitAndCheckStackStatus(cloudbreakClient, stackIdStr, "AVAILABLE")
        CloudbreakUtil.checkClusterAvailability(cloudbreakClient.stackEndpoint(), ambariPort, stackIdStr, ambariUser, ambariPassword, checkAmbari)
    }

    private fun convertHostGroups(hostGroups: List<HostGroup>, runRecipesOnHosts: String): Set<HostGroupJson> {
        var recipeIds: Set<Long>? = emptySet<Long>()
        var hostGroupsWithRecipe = emptyList<String>()
        if (!runRecipesOnHosts.isEmpty()) {
            recipeIds = itContext.getContextParam<Set<Any>>(CloudbreakITContextConstants.RECIPE_ID, Set<Any>::class.java)
            Assert.assertFalse(recipeIds == null || recipeIds.isEmpty())
            hostGroupsWithRecipe = Arrays.asList<String>(*runRecipesOnHosts.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())
        }
        val hgMaps = HashSet<HostGroupJson>()
        for (hostgroup in hostGroups) {
            val hostGroupJson = HostGroupJson()
            hostGroupJson.name = hostgroup.name


            val constraintJson = ConstraintJson()
            constraintJson.instanceGroupName = hostgroup.instanceGroupName
            constraintJson.hostCount = hostgroup.hostCount
            hostGroupJson.constraint = constraintJson
            if (hostGroupsWithRecipe.contains(hostgroup.name)) {
                hostGroupJson.recipeIds = recipeIds
            }
            hgMaps.add(hostGroupJson)
        }
        return hgMaps
    }
}
