package com.sequenceiq.it.cloudbreak.mock

import com.sequenceiq.it.spark.ITResponse.AMBARI_API_ROOT
import com.sequenceiq.it.spark.ITResponse.CONSUL_API_ROOT
import com.sequenceiq.it.spark.ITResponse.SWARM_API_ROOT
import spark.Spark.delete
import spark.Spark.get
import spark.Spark.post
import spark.Spark.put

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
import com.sequenceiq.it.spark.consul.ConsulEventFireResponse
import com.sequenceiq.it.spark.consul.ConsulKeyValueGetResponse
import com.sequenceiq.it.spark.docker.model.Info
import com.sequenceiq.it.spark.docker.model.InspectContainerResponse


class MockClusterCreationWithSwarmSuccessTest : AbstractMockIntegrationTest() {

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

        addSwarmMappings(numberOfServers)
        addConsulMappings()
        addAmbariMappings(numberOfServers)

        val clusterEndpoint = cloudbreakClient.clusterEndpoint()
        CloudbreakUtil.checkResponse("ClusterCreation", clusterEndpoint.post(java.lang.Long.valueOf(stackId!!.toLong()), clusterRequest))
        // THEN
        CloudbreakUtil.waitAndCheckStackStatus(cloudbreakClient, stackIdStr, "AVAILABLE")
        CloudbreakUtil.checkClusterAvailability(cloudbreakClient.stackEndpoint(), ambariPort, stackIdStr, ambariUser, ambariPassword, checkAmbari)

        verify(AMBARI_API_ROOT + "/views/FILES/versions/1.0.0/instances/files", "POST").exactTimes(1).bodyContains("ambari_cluster").verify()
        verify(AMBARI_API_ROOT + "/views/PIG/versions/1.0.0/instances/pig", "POST").exactTimes(1).bodyContains("ambari_cluster").verify()
        verify(AMBARI_API_ROOT + "/views/HIVE/versions/1.0.0/instances/hive", "POST").exactTimes(1).bodyContains("ambari_cluster").verify()
        verify(AMBARI_API_ROOT + "/services/AMBARI/components/AMBARI_SERVER", "GET").exactTimes(1).verify()
        verify(AMBARI_API_ROOT + "/clusters", "GET").exactTimes(1).verify()
        verify(AMBARI_API_ROOT + "/check", "GET").exactTimes(1).verify()
        verify(AMBARI_API_ROOT + "/users/admin", "PUT").exactTimes(1).bodyContains("Users/password").bodyContains("Users/old_password").verify()
        verify(AMBARI_API_ROOT + "/blueprints/bp", "POST").exactTimes(1).bodyContains("blueprint_name").bodyContains("stack_name").bodyContains("stack_version").bodyContains("host_groups").exactTimes(1).verify()
        verify(AMBARI_API_ROOT + "/clusters/it-mock-cluster", "POST").exactTimes(1).bodyContains("blueprint").bodyContains("default_password").bodyContains("host_groups").verify()
        verify(AMBARI_API_ROOT + "/clusters/ambari_cluster/requests/1", "GET").atLeast(1).verify()
        verify(AMBARI_API_ROOT + "/clusters/ambari_cluster/hosts", "GET").exactTimes(1).verify()

        verify(CONSUL_API_ROOT + "/event/fire/recipe-post-install", "PUT").exactTimes(1).verify()
        verify(CONSUL_API_ROOT + "/event/fire/cleanup-plugin", "PUT").atLeast(1).verify()
        verify(CONSUL_API_ROOT + "/event/fire/install-plugin", "PUT").exactTimes(1).verify()

        verifyRegexpPath(SWARM_API_ROOT + "/containers/logrotate.*/start", "POST").exactTimes(numberOfServers + 1).verify()
        verifyRegexpPath(SWARM_API_ROOT + "/containers/consul-watch.*/start", "POST").exactTimes(numberOfServers + 1).verify()
        verify("$SWARM_API_ROOT/containers/registrator-$clusterName/start", "POST").exactTimes(1).verify()
        verify("$SWARM_API_ROOT/containers/ambari_db-$clusterName/start", "POST").exactTimes(1).verify()
        verify("$SWARM_API_ROOT/containers/ambari-server-$clusterName/start", "POST").exactTimes(1).verify()
        verifyRegexpPath("$SWARM_API_ROOT/containers/ambari-agent-$clusterName.*/start", "POST").exactTimes(numberOfServers).verify()
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

    private fun addConsulMappings() {
        get(CONSUL_API_ROOT + "/kv/*", ConsulKeyValueGetResponse(), ResponseTransformer { gson().toJson(it) })
        put(CONSUL_API_ROOT + "/kv/*", { req, res -> java.lang.Boolean.TRUE }, ResponseTransformer { gson().toJson(it) })
        put(CONSUL_API_ROOT + "/event/fire/*", ConsulEventFireResponse(), ResponseTransformer { gson().toJson(it) })
    }

    private fun addSwarmMappings(numberOfServers: Int) {
        post(SWARM_API_ROOT + "/containers/:container/start") { req, res -> "" }
        get(SWARM_API_ROOT + "/info", { req, res -> Info(numberOfServers) }, ResponseTransformer { gson().toJson(it) })
        get(SWARM_API_ROOT + "/containers/:container/json", { req, res -> InspectContainerResponse("id") }, ResponseTransformer { gson().toJson(it) })
        delete(SWARM_API_ROOT + "/containers/:container") { req, res -> "" }
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
