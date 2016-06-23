package com.sequenceiq.cloudbreak.shell.commands.common

import java.util.HashSet

import org.apache.http.MethodNotSupportedException
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson
import com.sequenceiq.cloudbreak.api.model.ClusterRequest
import com.sequenceiq.cloudbreak.api.model.ClusterResponse
import com.sequenceiq.cloudbreak.api.model.ConfigStrategy
import com.sequenceiq.cloudbreak.api.model.ConstraintJson
import com.sequenceiq.cloudbreak.api.model.FileSystemRequest
import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson
import com.sequenceiq.cloudbreak.api.model.HostGroupJson
import com.sequenceiq.cloudbreak.api.model.RDSConfigJson
import com.sequenceiq.cloudbreak.api.model.RDSDatabase
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.api.model.StatusRequest
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands
import com.sequenceiq.cloudbreak.shell.completion.HostGroup
import com.sequenceiq.cloudbreak.shell.model.Hints
import com.sequenceiq.cloudbreak.shell.model.HostgroupEntry
import com.sequenceiq.cloudbreak.shell.model.MarathonHostgroupEntry
import com.sequenceiq.cloudbreak.shell.model.NodeCountEntry
import com.sequenceiq.cloudbreak.shell.model.ShellContext
import com.sequenceiq.cloudbreak.shell.util.CloudbreakShellUtil

class ClusterCommands(private val shellContext: ShellContext, private val cloudbreakShellUtil: CloudbreakShellUtil) : BaseCommands {

    @CliAvailabilityIndicator(value = "cluster create")
    fun createAvailable(): Boolean {
        return shellContext.isBlueprintAvailable && (shellContext.isStackAvailable && shellContext.activeHostGroups.size == shellContext.hostGroups.keys.size || shellContext.isMarathonMode && shellContext.isSelectedMarathonStackAvailable
                && shellContext.activeHostGroups.size == shellContext.marathonHostGroups.size)
    }

    @CliCommand(value = "cluster create", help = "Create a new cluster based on a blueprint and optionally a recipe")
    fun createCluster(
            @CliOption(key = "userName", mandatory = false, unspecifiedDefaultValue = "admin", help = "Username of the Ambari server") userName: String,
            @CliOption(key = "password", mandatory = false, unspecifiedDefaultValue = "admin", help = "Password of the Ambari server") password: String,
            @CliOption(key = "description", mandatory = false, help = "Description of the blueprint") description: String,
            @CliOption(key = "stack", mandatory = false, help = "Stack definition name, like HDP") stack: String?,
            @CliOption(key = "version", mandatory = false, help = "Stack definition version") version: String?,
            @CliOption(key = "os", mandatory = false, help = "Stack OS to select package manager, default is RedHat") os: String?,
            @CliOption(key = "stackRepoId", mandatory = false, help = "Stack repository id") stackRepoId: String?,
            @CliOption(key = "stackBaseURL", mandatory = false, help = "Stack url") stackBaseURL: String?,
            @CliOption(key = "utilsRepoId", mandatory = false, help = "Stack utils repoId") utilsRepoId: String?,
            @CliOption(key = "utilsBaseURL", mandatory = false, help = "Stack utils URL") utilsBaseURL: String?,
            @CliOption(key = "verify", mandatory = false, help = "Whether to verify the URLs or not") verify: Boolean?,
            @CliOption(key = "connectionURL", mandatory = false, help = "JDBC connection URL (jdbc:<db-type>://<address>:<port>/<db>)") connectionURL: String?,
            @CliOption(key = "databaseType", mandatory = false, help = "Type of the external database (MYSQL, POSTGRES)") databaseType: RDSDatabase?,
            @CliOption(key = "connectionUserName", mandatory = false, help = "Username to use for the jdbc connection") connectionUserName: String?,
            @CliOption(key = "connectionPassword", mandatory = false, help = "Password to use for the jdbc connection") connectionPassword: String?,
            @CliOption(key = "enableSecurity", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Kerberos security status")
            enableSecurity: Boolean?,
            @CliOption(key = "kerberosMasterKey", mandatory = false, specifiedDefaultValue = "key", help = "Kerberos mater key") kerberosMasterKey: String,
            @CliOption(key = "kerberosAdmin", mandatory = false, specifiedDefaultValue = "admin", help = "Kerberos admin name") kerberosAdmin: String,
            @CliOption(key = "kerberosPassword", mandatory = false, specifiedDefaultValue = "admin", help = "Kerberos admin password") kerberosPassword: String,
            @CliOption(key = "ldapRequired", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Start and configure LDAP authentication support for Ambari hosts")
            ldapRequired: Boolean?,
            @CliOption(key = "configStrategy", mandatory = false, help = "Config recommendation strategy") strategy: ConfigStrategy?,
            @CliOption(key = "enableShipyard", mandatory = false, help = "Run shipyard in cluster") enableShipyard: Boolean?,
            @CliOption(key = "wait", mandatory = false, help = "Wait for stack creation", specifiedDefaultValue = "false") wait: Boolean?): String {
        var wait = wait
        try {
            val hostGroupList = HashSet<HostGroupJson>()
            for (entry in if (shellContext.isMarathonMode)
                shellContext.marathonHostGroups.entries
            else
                shellContext.hostGroups.entries) {
                val hostGroupJson = HostGroupJson()
                hostGroupJson.name = entry.key

                val constraintJson = ConstraintJson()

                constraintJson.hostCount = entry.value.nodeCount
                if (shellContext.isMarathonMode) {
                    constraintJson.constraintTemplateName = (entry.value as MarathonHostgroupEntry).constraintName
                } else {
                    hostGroupJson.recipeIds = (entry.value as HostgroupEntry).recipeIdSet
                    constraintJson.instanceGroupName = entry.key
                }

                hostGroupJson.constraint = constraintJson
                hostGroupList.add(hostGroupJson)
            }

            wait = if (wait == null) false else wait
            val clusterRequest = ClusterRequest()
            clusterRequest.enableShipyard = enableShipyard ?: false
            clusterRequest.name = if (shellContext.isMarathonMode) shellContext.selectedMarathonStackName else shellContext.stackName
            clusterRequest.description = description
            clusterRequest.userName = userName
            clusterRequest.password = password
            clusterRequest.blueprintId = java.lang.Long.valueOf(shellContext.blueprintId)
            clusterRequest.emailNeeded = false
            clusterRequest.enableSecurity = enableSecurity
            clusterRequest.hostGroups = hostGroupList

            if (strategy != null) {
                clusterRequest.configStrategy = strategy
            }

            if (!shellContext.isMarathonMode) {
                var fileSystemRequest: FileSystemRequest? = FileSystemRequest()
                fileSystemRequest!!.name = shellContext.stackName
                fileSystemRequest.isDefaultFs = if (shellContext.defaultFileSystem == null) true else shellContext.defaultFileSystem
                fileSystemRequest.type = shellContext.fileSystemType

                if (shellContext.defaultFileSystem == null && shellContext.fileSystemType == null) {
                    fileSystemRequest = null
                }
                clusterRequest.fileSystem = fileSystemRequest
            }
            clusterRequest.kerberosAdmin = kerberosAdmin
            clusterRequest.kerberosMasterKey = kerberosMasterKey
            clusterRequest.kerberosPassword = kerberosPassword
            clusterRequest.ldapRequired = ldapRequired
            if (shellContext.sssdConfigId != null) {
                clusterRequest.sssdConfigId = java.lang.Long.valueOf(shellContext.sssdConfigId)
            }
            clusterRequest.setValidateBlueprint(false)

            var ambariStackDetailsJson: AmbariStackDetailsJson? = AmbariStackDetailsJson()
            ambariStackDetailsJson!!.os = os
            ambariStackDetailsJson.stack = stack
            ambariStackDetailsJson.stackBaseURL = stackBaseURL
            ambariStackDetailsJson.stackRepoId = stackRepoId
            ambariStackDetailsJson.utilsBaseURL = utilsBaseURL
            ambariStackDetailsJson.utilsRepoId = utilsRepoId
            ambariStackDetailsJson.verify = verify
            ambariStackDetailsJson.version = version

            if (os == null && stack == null && stackBaseURL == null && stackRepoId == null && utilsBaseURL == null
                    && utilsRepoId == null && verify == null && version == null) {
                ambariStackDetailsJson = null
            }
            clusterRequest.ambariStackDetails = ambariStackDetailsJson

            if (connectionURL != null && connectionUserName != null && connectionPassword != null && databaseType != null) {
                val rdsConfigJson = RDSConfigJson()
                rdsConfigJson.connectionURL = connectionURL
                rdsConfigJson.databaseType = databaseType
                rdsConfigJson.connectionUserName = connectionUserName
                rdsConfigJson.connectionPassword = connectionPassword
                clusterRequest.rdsConfigJson = rdsConfigJson
            } else if (connectionURL != null || connectionUserName != null || connectionPassword != null || databaseType != null) {
                return "connectionURL, databaseType, connectionUserName and connectionPassword must be all set."
            }

            val stackId = if (shellContext.isMarathonMode) shellContext.selectedMarathonStackId!!.toString() else shellContext.stackId
            cloudbreakShellUtil.checkResponse("createCluster", shellContext.cloudbreakClient().clusterEndpoint().post(java.lang.Long.valueOf(stackId), clusterRequest))
            shellContext.setHint(Hints.NONE)
            shellContext.resetFileSystemConfiguration()
            if (wait) {
                val waitResult = cloudbreakShellUtil.waitAndCheckClusterStatus(java.lang.Long.valueOf(stackId), Status.AVAILABLE.name)
                if (CloudbreakShellUtil.WaitResultStatus.FAILED == waitResult.waitResultStatus) {
                    throw shellContext.exceptionTransformer().transformToRuntimeException(
                            String.format("Cluster creation failed on stack with id: '%s': '%s'", stackId, waitResult.reason))
                } else {
                    return "Cluster creation finished"
                }
            }
            return "Cluster creation started"
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = *arrayOf("cluster stop", "cluster start"))
    fun startStopAvailable(): Boolean {
        return shellContext.isStackAvailable || shellContext.isMarathonMode && shellContext.isSelectedMarathonStackAvailable
    }

    @CliCommand(value = "cluster stop", help = "Stop your cluster")
    fun stop(): String {
        try {
            val updateClusterJson = UpdateClusterJson()
            updateClusterJson.status = StatusRequest.STOPPED
            val stackId = if (shellContext.isMarathonMode) shellContext.selectedMarathonStackId!!.toString() else shellContext.stackId
            cloudbreakShellUtil.checkResponse("stopCluster",
                    shellContext.cloudbreakClient().clusterEndpoint().put(java.lang.Long.valueOf(stackId), updateClusterJson))
            return "Cluster is stopping"
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "cluster start", help = "Start your cluster")
    fun start(): String {
        try {
            val updateClusterJson = UpdateClusterJson()
            updateClusterJson.status = StatusRequest.STARTED
            val stackId = if (shellContext.isMarathonMode) shellContext.selectedMarathonStackId!!.toString() else shellContext.stackId
            cloudbreakShellUtil.checkResponse("startCluster",
                    shellContext.cloudbreakClient().clusterEndpoint().put(java.lang.Long.valueOf(stackId), updateClusterJson))
            return "Cluster is starting"
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    override fun deleteAvailable(): Boolean {
        return false
    }

    @Throws(Exception::class)
    override fun delete(id: Long?, name: String?): String {
        throw MethodNotSupportedException("Cluster delete command not available")
    }

    @CliCommand(value = "cluster delete", help = "Delete the cluster by stack id")
    fun delete(): String {
        try {
            val stackId = if (shellContext.isMarathonMode) shellContext.selectedMarathonStackId!!.toString() else shellContext.stackId
            shellContext.cloudbreakClient().clusterEndpoint().delete(java.lang.Long.valueOf(stackId))
            return "Cluster deletion started with stack id: " + stackId
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @Throws(Exception::class)
    override fun deleteById(id: Long?): String {
        return delete(id, null)
    }

    @Throws(Exception::class)
    override fun deleteByName(name: String): String {
        return delete(null, name)
    }

    override fun selectAvailable(): Boolean {
        return false
    }

    @Throws(Exception::class)
    override fun select(id: Long?, name: String?): String {
        throw MethodNotSupportedException("Cluster select command not available")
    }

    @Throws(Exception::class)
    override fun selectById(id: Long?): String {
        return select(id, null)
    }

    @Throws(Exception::class)
    override fun selectByName(name: String): String {
        return select(null, name)
    }

    override fun listAvailable(): Boolean {
        return false
    }

    @Throws(Exception::class)
    override fun list(): String {
        throw MethodNotSupportedException("Cluster list command not available")
    }

    @CliAvailabilityIndicator(value = *arrayOf("cluster show --id", "cluster show --name"))
    override fun showAvailable(): Boolean {
        return shellContext.isStackAvailable || shellContext.isMarathonMode && shellContext.isSelectedMarathonStackAvailable
    }

    @CliCommand(value = "cluster show", help = "Shows the cluster by stack id")
    fun show(): String {
        try {
            val stackId = if (shellContext.isMarathonMode) shellContext.selectedMarathonStackId!!.toString() else shellContext.stackId
            val clusterResponse = shellContext.cloudbreakClient().clusterEndpoint()[java.lang.Long.valueOf(stackId)]
            return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(clusterResponse), "FIELD", "VALUE")
        } catch (ex: IndexOutOfBoundsException) {
            throw shellContext.exceptionTransformer().transformToRuntimeException("There was no cluster for this account.")
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @Throws(Exception::class)
    override fun show(id: Long?, name: String?): String {
        throw MethodNotSupportedException("Cluster show command not available")
    }

    @Throws(Exception::class)
    override fun showById(id: Long?): String {
        return show(id, null)
    }

    @Throws(Exception::class)
    override fun showByName(name: String): String {
        return show(null, name)
    }

    @CliAvailabilityIndicator(value = *arrayOf("cluster node"))
    fun nodeAvailable(): Boolean {
        return shellContext.isStackAvailable || shellContext.isMarathonMode && shellContext.isSelectedMarathonStackAvailable
    }

    @CliAvailabilityIndicator(value = *arrayOf("cluster fileSystem"))
    fun fileSystemAvailable(): Boolean {
        return shellContext.isStackAvailable && !shellContext.isMarathonMode
    }

    @CliCommand(value = "cluster node --ADD", help = "Add new nodes to the cluster")
    fun addNode(
            @CliOption(key = "hostgroup", mandatory = true, help = "Name of the hostgroup") hostGroup: HostGroup,
            @CliOption(key = "adjustment", mandatory = true, help = "Count of the nodes which will be added to the cluster") adjustment: Int?): String {
        try {
            if (adjustment < 1) {
                return "The adjustment value in case of node addition should be at least 1."
            }
            val updateClusterJson = UpdateClusterJson()
            val hostGroupAdjustmentJson = HostGroupAdjustmentJson()
            hostGroupAdjustmentJson.scalingAdjustment = adjustment
            hostGroupAdjustmentJson.withStackUpdate = false
            hostGroupAdjustmentJson.hostGroup = hostGroup.name
            updateClusterJson.hostGroupAdjustment = hostGroupAdjustmentJson
            val stackId = if (shellContext.isMarathonMode) shellContext.selectedMarathonStackId!!.toString() else shellContext.stackId
            cloudbreakShellUtil.checkResponse("upscaleCluster",
                    shellContext.cloudbreakClient().clusterEndpoint().put(java.lang.Long.valueOf(stackId), updateClusterJson))
            return shellContext.stackId
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "cluster node --REMOVE", help = "Remove nodes from the cluster")
    fun removeNode(
            @CliOption(key = "hostgroup", mandatory = true, help = "Name of the hostgroup") hostGroup: HostGroup,
            @CliOption(key = "adjustment", mandatory = true, help = "The number of the nodes to be removed from the cluster.") adjustment: Int?,
            @CliOption(key = "withStackDownScale", mandatory = false, help = "Do the downscale with the stack together") withStackDownScale: Boolean?): String {
        try {
            if (adjustment > -1) {
                return "The adjustment value in case of node removal should be negative."
            }
            val updateClusterJson = UpdateClusterJson()
            val hostGroupAdjustmentJson = HostGroupAdjustmentJson()
            hostGroupAdjustmentJson.scalingAdjustment = adjustment
            hostGroupAdjustmentJson.withStackUpdate = withStackDownScale ?: false
            hostGroupAdjustmentJson.hostGroup = hostGroup.name
            updateClusterJson.hostGroupAdjustment = hostGroupAdjustmentJson
            val stackId = if (shellContext.isMarathonMode) shellContext.selectedMarathonStackId!!.toString() else shellContext.stackId
            cloudbreakShellUtil.checkResponse("downscaleCluster",
                    shellContext.cloudbreakClient().clusterEndpoint().put(java.lang.Long.valueOf(stackId), updateClusterJson))
            return shellContext.stackId
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = "cluster sync")
    fun syncAvailable(): Boolean {
        return shellContext.isStackAvailable && !shellContext.isMarathonMode
    }

    @CliCommand(value = "cluster sync", help = "Sync the cluster")
    fun sync(): String {
        try {
            val updateClusterJson = UpdateClusterJson()
            updateClusterJson.status = StatusRequest.SYNC
            shellContext.cloudbreakClient().clusterEndpoint().put(java.lang.Long.valueOf(shellContext.stackId), updateClusterJson)
            return "Cluster is syncing"
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    override fun shellContext(): ShellContext {
        return shellContext
    }

}
