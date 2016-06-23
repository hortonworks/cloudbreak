package com.sequenceiq.it.cloudbreak.mock

import com.sequenceiq.it.spark.ITResponse.AMBARI_API_ROOT
import com.sequenceiq.it.spark.ITResponse.SALT_API_ROOT
import com.sequenceiq.it.spark.ITResponse.SALT_BOOT_ROOT
import spark.Spark.get
import spark.Spark.post
import spark.Spark.put

import java.util.Arrays
import java.util.Collections
import java.util.HashSet

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Optional
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.ObjectMapper
import com.sequenceiq.cloudbreak.api.endpoint.ClusterEndpoint
import com.sequenceiq.cloudbreak.api.model.ClusterRequest
import com.sequenceiq.cloudbreak.api.model.ConstraintJson
import com.sequenceiq.cloudbreak.api.model.HostGroupJson
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse
import com.sequenceiq.it.IntegrationTestContext
import com.sequenceiq.it.cloudbreak.AbstractMockIntegrationTest
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants
import com.sequenceiq.it.cloudbreak.CloudbreakUtil
import com.sequenceiq.it.cloudbreak.HostGroup
import com.sequenceiq.it.spark.ambari.AmbariBlueprintsResponse
import com.sequenceiq.it.spark.ambari.AmbariCheckResponse
import com.sequenceiq.it.spark.ambari.AmbariClusterRequestsResponse
import com.sequenceiq.it.spark.ambari.AmbariClusterResponse
import com.sequenceiq.it.spark.ambari.AmbariClustersHostsResponse
import com.sequenceiq.it.spark.ambari.AmbariHostsResponse
import com.sequenceiq.it.spark.ambari.AmbariServicesComponentsResponse
import com.sequenceiq.it.spark.ambari.AmbariStatusResponse
import com.sequenceiq.it.spark.ambari.EmptyAmbariResponse
import com.sequenceiq.it.spark.salt.SaltApiRunPostResponse


class MockClusterCreationWithSaltFailTest : AbstractMockIntegrationTest() {

    @BeforeMethod
    fun setContextParameters() {
        val itContext = itContext
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.BLUEPRINT_ID), "Blueprint id is mandatory.")
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.")
    }

    @Test
    @Parameters("clusterName", "ambariPort", "ambariUser", "ambariPassword", "emailNeeded", "enableSecurity", "kerberosMasterKey", "kerberosAdmin", "kerberosPassword", "runRecipesOnHosts", "checkAmbari", "mockPort")
    @Throws(Exception::class)
    fun testClusterCreation(@Optional("it-cluster") clusterName: String, @Optional("8080") ambariPort: String, @Optional("admin") ambariUser: String,
                            @Optional("admin123!@#") ambariPassword: String, @Optional("false") emailNeeded: Boolean,
                            @Optional("false") enableSecurity: Boolean, @Optional kerberosMasterKey: String, @Optional kerberosAdmin: String, @Optional kerberosPassword: String,
                            @Optional("") runRecipesOnHosts: String, @Optional("true") checkAmbari: Boolean, @Optional("443") mockPort: Int) {
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

        var numberOfServers = 0
        for (hostgroup in hostgroups) {
            numberOfServers += hostgroup.hostCount!!
        }

        initSpark()

        addSaltMappings(numberOfServers + 1)
        addAmbariMappings(numberOfServers)

        val clusterEndpoint = cloudbreakClient.clusterEndpoint()
        CloudbreakUtil.checkResponse("ClusterCreation", clusterEndpoint.post(java.lang.Long.valueOf(stackId!!.toLong()), clusterRequest))
        // THEN
        CloudbreakUtil.waitAndCheckStackStatus(cloudbreakClient, stackIdStr, "AVAILABLE")
        val failMessage = "Source file salt://ambari/scripts/ambari-server-initttt.sh not found | "
        +"Service ambari-server is already enabled, and is dead | "
        +"Package haveged is already installed."
        CloudbreakUtil.checkClusterFailed(cloudbreakClient.stackEndpoint(), stackIdStr, failMessage)
    }

    private fun addAmbariMappings(numberOfServers: Int) {
        get(AMBARI_API_ROOT + "/clusters/:cluster/requests/:request", AmbariStatusResponse())
        post(AMBARI_API_ROOT + "/views/:view/versions/1.0.0/instances/*", EmptyAmbariResponse())
        get(AMBARI_API_ROOT + "/clusters", AmbariClusterResponse(numberOfServers))
        post(AMBARI_API_ROOT + "/clusters/:cluster/requests", AmbariClusterRequestsResponse())
        post(AMBARI_API_ROOT + "/clusters/:cluster", EmptyAmbariResponse(), ResponseTransformer { gson().toJson(it) })
        get(AMBARI_API_ROOT + "/services/AMBARI/components/AMBARI_SERVER", AmbariServicesComponentsResponse(), ResponseTransformer { gson().toJson(it) })
        get(AMBARI_API_ROOT + "/hosts", AmbariHostsResponse(numberOfServers), ResponseTransformer { gson().toJson(it) })
        get(AMBARI_API_ROOT + "/blueprints/*", AmbariBlueprintsResponse())
        post(AMBARI_API_ROOT + "/blueprints/*", EmptyAmbariResponse())
        put(AMBARI_API_ROOT + "/users/admin", EmptyAmbariResponse())
        get(AMBARI_API_ROOT + "/check", AmbariCheckResponse())
        get(AMBARI_API_ROOT + "/clusters/:cluster/hosts", AmbariClustersHostsResponse(numberOfServers))
    }

    private fun addSaltMappings(numberOfServers: Int) {
        val objectMapper = ObjectMapper()
        objectMapper.setVisibility(objectMapper.visibilityChecker.withGetterVisibility(JsonAutoDetect.Visibility.NONE))
        post(SALT_API_ROOT + "/run", object : SaltApiRunPostResponse(numberOfServers) {
            override fun jobsLookupJid(): Any {
                return ITResponse.responseFromJsonFile("saltapi/lookup_jid_fail_response.json")
            }
        })
        post(SALT_BOOT_ROOT + "/salt/server/pillar", { request, response ->
            val genericResponse = GenericResponse()
            genericResponse.statusCode = HttpStatus.OK.value()
            genericResponse
        }, ResponseTransformer { gson().toJson(it) })
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

    companion object {

        private val LOGGER = LoggerFactory.getLogger(MockClusterCreationWithSaltFailTest::class.java)
    }
}
