package com.sequenceiq.cloudbreak.shell.commands.base

import java.util.ArrayList
import java.util.HashMap

import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption

import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.api.model.ClusterResponse
import com.sequenceiq.cloudbreak.api.model.FailurePolicyJson
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.api.model.InstanceMetaDataJson
import com.sequenceiq.cloudbreak.api.model.OnFailureAction
import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest
import com.sequenceiq.cloudbreak.api.model.StackRequest
import com.sequenceiq.cloudbreak.api.model.StackResponse
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.api.model.StatusRequest
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands
import com.sequenceiq.cloudbreak.shell.commands.StackCommands
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroup
import com.sequenceiq.cloudbreak.shell.completion.PlatformVariant
import com.sequenceiq.cloudbreak.shell.completion.StackAvailabilityZone
import com.sequenceiq.cloudbreak.shell.completion.StackRegion
import com.sequenceiq.cloudbreak.shell.exception.ValidationException
import com.sequenceiq.cloudbreak.shell.model.Hints
import com.sequenceiq.cloudbreak.shell.model.InstanceGroupEntry
import com.sequenceiq.cloudbreak.shell.model.OutPutType
import com.sequenceiq.cloudbreak.shell.model.ShellContext
import com.sequenceiq.cloudbreak.shell.util.CloudbreakShellUtil

class BaseStackCommands(private val shellContext: ShellContext, private val cloudbreakShellUtil: CloudbreakShellUtil) : BaseCommands, StackCommands {

    @CliAvailabilityIndicator(value = "stack list")
    override fun listAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    @CliCommand(value = "stack list", help = "Shows all of your stack")
    override fun list(): String {
        try {
            val publics = shellContext.cloudbreakClient().stackEndpoint().publics
            return shellContext.outputTransformer().render(shellContext.responseTransformer().transformToMap(publics, "id", "name"), "ID", "INFO")
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator("stack show --id", "stack show --name")
    override fun showAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    @CliCommand(value = "stack show --id", help = "Show the stack by its id")
    @Throws(Exception::class)
    override fun showById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return show(id, null)
    }

    @CliCommand(value = "stack show --name", help = "Show the stack by its name")
    @Throws(Exception::class)
    override fun showByName(@CliOption(key = "", mandatory = true) name: String): String {
        return show(null, name)
    }

    override fun show(id: Long?, name: String?): String {
        try {
            val stackResponse = getStackResponse(name, id)
            if (stackResponse != null) {
                return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(stackResponse), "FIELD", "VALUE")
            }
            return "No stack specified (select a stack by --id or --name)."
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = *arrayOf("stack delete --id", "stack delete --name"))
    override fun deleteAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    @Throws(Exception::class)
    override fun deleteById(id: Long?): String {
        return delete(id, null)
    }

    @Throws(Exception::class)
    override fun deleteByName(name: String): String {
        return delete(null, name)
    }

    @CliCommand(value = "stack delete --id", help = "Delete the stack by its id")
    @Throws(Exception::class)
    fun deleteByName(
            @CliOption(key = "", mandatory = true) id: Long?,
            @CliOption(key = "wait", mandatory = false, help = "Wait for stack termination", specifiedDefaultValue = "false") wait: Boolean?): String {
        return delete(id, null, wait)
    }

    @CliCommand(value = "stack delete --name", help = "Delete the stack by its name")
    @Throws(Exception::class)
    fun deleteById(
            @CliOption(key = "", mandatory = true) name: String,
            @CliOption(key = "wait", mandatory = false, help = "Wait for stack termination", specifiedDefaultValue = "false") wait: Boolean?): String {
        return delete(null, name, wait)
    }

    @Throws(Exception::class)
    override fun delete(id: Long?, name: String?): String {
        return delete(id, name, false)
    }

    fun delete(id: Long?, name: String?, wait: Boolean?): String {
        var wait = wait
        try {
            wait = if (wait == null) false else wait
            if (id != null) {
                shellContext.cloudbreakClient().stackEndpoint().delete(java.lang.Long.valueOf(id), false)
                shellContext.setHint(Hints.CREATE_CLUSTER)
                shellContext.removeStack(id.toString())
                if (wait) {
                    val waitResult = cloudbreakShellUtil.waitAndCheckStackStatus(java.lang.Long.valueOf(id), Status.DELETE_COMPLETED.name)
                    if (CloudbreakShellUtil.WaitResultStatus.FAILED == waitResult.waitResultStatus) {
                        throw shellContext.exceptionTransformer().transformToRuntimeException("Stack termination failed: " + waitResult.reason)
                    } else {
                        return "Stack terminated with id: " + id
                    }
                } else {
                    return "Stack termination started with id: " + id
                }
            } else if (name != null) {
                val response = shellContext.cloudbreakClient().stackEndpoint().getPublic(name)
                shellContext.cloudbreakClient().stackEndpoint().deletePublic(name, false)
                shellContext.setHint(Hints.CREATE_CLUSTER)

                if (wait) {
                    val waitResult = cloudbreakShellUtil.waitAndCheckStackStatus(response.id, Status.DELETE_COMPLETED.name)
                    if (CloudbreakShellUtil.WaitResultStatus.FAILED == waitResult.waitResultStatus) {
                        throw shellContext.exceptionTransformer().transformToRuntimeException("Stack termination failed: " + waitResult.reason)
                    } else {
                        return "Stack terminated with name: " + name
                    }
                } else {
                    return "Stack termination started with name: " + name
                }
            }
            return "Stack not specified. (select by using --id or --name)"
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = *arrayOf("stack select --id", "stack select --name"))
    override fun selectAvailable(): Boolean {
        return shellContext.isStackAccessible && !shellContext.isMarathonMode
    }

    override fun select(id: Long?, name: String?): String {
        try {
            if (id != null) {
                val stack = shellContext.cloudbreakClient().stackEndpoint()[id]
                if (stack != null) {
                    shellContext.addStack(id.toString(), stack.name)
                    if (shellContext.isCredentialAvailable) {
                        shellContext.setHint(Hints.CREATE_CLUSTER)
                    } else {
                        shellContext.setHint(Hints.CONFIGURE_HOSTGROUP)
                    }
                    prepareCluster(id.toString())
                    shellContext.prepareInstanceGroups(stack)
                    return "Stack selected, id: " + id
                }

            } else if (name != null) {
                val stack = shellContext.cloudbreakClient().stackEndpoint().getPublic(name)
                if (stack != null) {
                    val stackId = stack.id
                    shellContext.addStack(stackId!!.toString(), name)
                    if (shellContext.isCredentialAvailable) {
                        shellContext.setHint(Hints.CREATE_CLUSTER)
                    } else {
                        shellContext.setHint(Hints.CONFIGURE_HOSTGROUP)
                    }
                    prepareCluster(stackId.toString())
                    shellContext.prepareInstanceGroups(stack)
                    return "Stack selected, name: " + name
                }
            }
            return "No stack specified. (select by using --id or --name)"
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "stack select --id", help = "Delete the stack by its id")
    @Throws(Exception::class)
    override fun selectById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return select(id, null)
    }

    @CliCommand(value = "stack select --name", help = "Delete the stack by its name")
    @Throws(Exception::class)
    override fun selectByName(@CliOption(key = "", mandatory = true) name: String): String {
        return select(null, name)
    }

    override fun createStackAvailable(platform: String): Boolean {
        return shellContext.isCredentialAvailable
                && shellContext.activeCloudPlatform == platform
                && shellContext.activeNetworkId != null
                && shellContext.activeSecurityGroupId != null
                && shellContext.activeHostGroups.size == shellContext.instanceGroups.size && shellContext.activeHostGroups.size != 0 && !shellContext.isMarathonMode
    }

    override fun create(name: String, region: StackRegion, availabilityZone: StackAvailabilityZone?, publicInAccount: Boolean?, onFailureAction: OnFailureAction?,
                        adjustmentType: AdjustmentType?, threshold: Long?, relocateDocker: Boolean?, wait: Boolean?, platformVariant: PlatformVariant?, orchestrator: String,
                        platform: String, ambariVersion: String, hdpVersion: String, params: Map<String, String>): String {
        var publicInAccount = publicInAccount
        var wait = wait
        try {
            validateNetwork()
            validateSecurityGroup()
            validateRegion(region)
            validateInstanceGroups()
            validateAvailabilityZone(region, availabilityZone)
            publicInAccount = if (publicInAccount == null) false else publicInAccount
            wait = if (wait == null) false else wait
            val id: IdJson
            val stackRequest = StackRequest()
            stackRequest.name = name
            stackRequest.region = region.name
            stackRequest.relocateDocker = relocateDocker
            if (availabilityZone != null) {
                stackRequest.availabilityZone = availabilityZone.name
            }
            stackRequest.onFailureAction = if (onFailureAction == null) OnFailureAction.DO_NOTHING else OnFailureAction.valueOf(onFailureAction.name)
            stackRequest.securityGroupId = java.lang.Long.valueOf(shellContext.activeSecurityGroupId!!)
            stackRequest.networkId = java.lang.Long.valueOf(shellContext.activeNetworkId!!)
            val failurePolicyJson = FailurePolicyJson()
            stackRequest.credentialId = java.lang.Long.valueOf(shellContext.credentialId)
            failurePolicyJson.adjustmentType = if (adjustmentType == null) AdjustmentType.BEST_EFFORT else AdjustmentType.valueOf(adjustmentType.name)
            failurePolicyJson.setThreshold(threshold ?: 1L)
            stackRequest.failurePolicy = failurePolicyJson
            stackRequest.platformVariant = if (platformVariant == null) "" else platformVariant.name
            stackRequest.cloudPlatform = platform
            stackRequest.parameters = params
            stackRequest.ambariVersion = ambariVersion
            stackRequest.hdpVersion = hdpVersion
            val orchestratorRequest = OrchestratorRequest()
            orchestratorRequest.type = orchestrator
            stackRequest.orchestrator = orchestratorRequest
            val instanceGroupJsonList = ArrayList<InstanceGroupJson>()
            for (stringObjectEntry in shellContext.instanceGroups.entries) {
                val instanceGroupEntry = stringObjectEntry.value
                val instanceGroupJson = InstanceGroupJson()
                instanceGroupJson.type = InstanceGroupType.valueOf(instanceGroupEntry.type)
                instanceGroupJson.templateId = instanceGroupEntry.templateId
                instanceGroupJson.nodeCount = instanceGroupEntry.nodeCount
                instanceGroupJson.group = stringObjectEntry.key
                instanceGroupJsonList.add(instanceGroupJson)
            }
            stackRequest.instanceGroups = instanceGroupJsonList

            if (publicInAccount) {
                id = shellContext.cloudbreakClient().stackEndpoint().postPublic(stackRequest)
            } else {
                id = shellContext.cloudbreakClient().stackEndpoint().postPrivate(stackRequest)
            }
            val stackResponse = StackResponse()
            stackResponse.name = stackRequest.name
            stackResponse.id = id.id
            shellContext.addStack(id.id!!.toString(), name)
            shellContext.setHint(Hints.CREATE_CLUSTER)

            if (wait) {
                val waitResult = cloudbreakShellUtil.waitAndCheckStackStatus(id.id, Status.AVAILABLE.name)
                if (CloudbreakShellUtil.WaitResultStatus.FAILED == waitResult.waitResultStatus) {
                    throw shellContext.exceptionTransformer().transformToRuntimeException("Stack creation failed:" + waitResult.reason)
                } else {
                    return "Stack creation finished with name: " + name
                }
            }
            return String.format("Stack creation started with id: '%s' and name: '%s'", id.id, name)
        } catch (ex: ValidationException) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator("stack node", "stack stop --id", "stack stop --name", "stack start --id", "stack start --name")
    fun nodeAvailable(): Boolean {
        return shellContext.isStackAvailable && !shellContext.isMarathonMode
    }

    @Throws(Exception::class)
    private fun stop(stackResponse: StackResponse): String {
        shellContext.addStack(stackResponse.id!!.toString(), stackResponse.name)
        prepareCluster(stackResponse.id!!.toString())
        val updateStackJson = UpdateStackJson()
        updateStackJson.status = StatusRequest.STOPPED
        cloudbreakShellUtil.checkResponse("stopStack",
                shellContext.cloudbreakClient().stackEndpoint().put(java.lang.Long.valueOf(shellContext.stackId), updateStackJson))
        return "Stack is stopping"
    }

    fun stop(id: Long?, name: String?): String {
        try {
            if (id != null) {
                val stack = shellContext.cloudbreakClient().stackEndpoint()[id]
                if (stack != null) {
                    return stop(stack)
                }
            } else if (name != null) {
                val stack = shellContext.cloudbreakClient().stackEndpoint().getPublic(name)
                if (stack != null) {
                    return stop(stack)
                }
            }
            return "Stack was not specified"
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "stack stop --id", help = "Stop the stack by its id")
    @Throws(Exception::class)
    fun stopById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return stop(id, null)
    }

    @CliCommand(value = "stack stop --name", help = "Stop the stack by its name")
    @Throws(Exception::class)
    fun stopByName(@CliOption(key = "", mandatory = true) name: String): String {
        return stop(null, name)
    }

    @Throws(Exception::class)
    private fun start(stackResponse: StackResponse): String {
        shellContext.addStack(stackResponse.id!!.toString(), stackResponse.name)
        prepareCluster(stackResponse.id!!.toString())
        val updateStackJson = UpdateStackJson()
        updateStackJson.status = StatusRequest.STARTED
        cloudbreakShellUtil.checkResponse("startStack",
                shellContext.cloudbreakClient().stackEndpoint().put(java.lang.Long.valueOf(shellContext.stackId), updateStackJson))
        return "Stack is starting"
    }

    fun start(id: Long?, name: String?): String {
        try {
            if (id != null) {
                val stack = shellContext.cloudbreakClient().stackEndpoint()[id]
                if (stack != null) {
                    return start(stack)
                }
            } else if (name != null) {
                val stack = shellContext.cloudbreakClient().stackEndpoint().getPublic(name)
                if (stack != null) {
                    return start(stack)
                }
            }
            return "Stack was not specified"
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "stack start --id", help = "Start the stack by its id")
    @Throws(Exception::class)
    fun startById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return start(id, null)
    }

    @CliCommand(value = "stack start --name", help = "Start the stack by its name")
    @Throws(Exception::class)
    fun startByName(@CliOption(key = "", mandatory = true) name: String): String {
        return start(null, name)
    }

    @CliCommand(value = "stack node --ADD", help = "Add new nodes to the cluster")
    fun addNode(
            @CliOption(key = "instanceGroup", mandatory = true, help = "Name of the instanceGroup") instanceGroup: InstanceGroup,
            @CliOption(key = "adjustment", mandatory = true, help = "Count of the nodes which will be added to the stack") adjustment: Int?,
            @CliOption(key = "withClusterUpScale", mandatory = false, help = "Do the upscale with the cluster together") withClusterUpScale: Boolean?): String {
        try {
            if (adjustment < 1) {
                return "The adjustment value in case of node addition should be at least 1."
            }
            val updateStackJson = UpdateStackJson()
            val instanceGroupAdjustmentJson = InstanceGroupAdjustmentJson()
            instanceGroupAdjustmentJson.scalingAdjustment = adjustment
            instanceGroupAdjustmentJson.withClusterEvent = withClusterUpScale ?: false
            instanceGroupAdjustmentJson.instanceGroup = instanceGroup.name
            updateStackJson.instanceGroupAdjustment = instanceGroupAdjustmentJson
            cloudbreakShellUtil.checkResponse("upscaleStack",
                    shellContext.cloudbreakClient().stackEndpoint().put(java.lang.Long.valueOf(shellContext.stackId), updateStackJson))
            return shellContext.stackId
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "stack node --REMOVE", help = "Remove nodes from the cluster")
    fun removeNode(
            @CliOption(key = "instanceGroup", mandatory = true, help = "Name of the instanceGroup") instanceGroup: InstanceGroup,
            @CliOption(key = "adjustment", mandatory = true, help = "Count of the nodes which will be removed to the stack") adjustment: Int?): String {
        try {
            if (adjustment > -1) {
                return "The adjustment value in case of node removal should be negative."
            }
            val updateStackJson = UpdateStackJson()
            val instanceGroupAdjustmentJson = InstanceGroupAdjustmentJson()
            instanceGroupAdjustmentJson.scalingAdjustment = adjustment
            instanceGroupAdjustmentJson.withClusterEvent = false
            instanceGroupAdjustmentJson.instanceGroup = instanceGroup.name
            updateStackJson.instanceGroupAdjustment = instanceGroupAdjustmentJson
            cloudbreakShellUtil.checkResponse("downscaleStack",
                    shellContext.cloudbreakClient().stackEndpoint().put(java.lang.Long.valueOf(shellContext.stackId), updateStackJson))
            return shellContext.stackId
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = "stack metadata")
    fun metadataAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    @CliCommand(value = "stack metadata", help = "Shows the stack metadata")
    fun metadata(
            @CliOption(key = "id", mandatory = false, help = "Id of the stack") id: Long?,
            @CliOption(key = "name", mandatory = false, help = "Name of the stack") name: String,
            @CliOption(key = "instancegroup", mandatory = false, help = "Instancegroup of the stack") group: String,
            @CliOption(key = "outputType", mandatory = false, help = "OutputType of the response") outPutType: OutPutType?): String {
        var outPutType = outPutType
        try {
            outPutType = if (outPutType == null) OutPutType.RAW else outPutType
            val stackResponse = getStackResponse(name, id)
            if (stackResponse != null && stackResponse.instanceGroups != null) {
                val stringListMap = collectMetadata(
                        if (stackResponse.instanceGroups == null) ArrayList<InstanceGroupJson>() else stackResponse.instanceGroups, group)
                return shellContext.outputTransformer().render(outPutType, stringListMap, "FIELD", "VALUE")
            }
            return "No stack specified."
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = "stack sync")
    fun syncAvailable(): Boolean {
        return shellContext.isStackAvailable && !shellContext.isMarathonMode
    }

    @CliCommand(value = "stack sync", help = "Sync the stack")
    fun sync(): String {
        try {
            val updateStackJson = UpdateStackJson()
            updateStackJson.status = StatusRequest.SYNC
            shellContext.cloudbreakClient().stackEndpoint().put(java.lang.Long.valueOf(shellContext.stackId), updateStackJson)
            return "Stack is syncing"
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    override fun shellContext(): ShellContext {
        return shellContext
    }

    private fun getStackResponse(name: String?, id: Long?): StackResponse? {
        if (name != null) {
            return shellContext.cloudbreakClient().stackEndpoint().getPublic(name)
        } else if (id != null) {
            return shellContext.cloudbreakClient().stackEndpoint()[id]
        }
        return null
    }

    private fun collectMetadata(instanceGroups: List<InstanceGroupJson>, group: String): Map<String, List<String>> {
        val returnValues = HashMap<String, List<String>>()
        for (instanceGroup in instanceGroups) {
            val list = ArrayList<String>()
            for (instanceMetaDataJson in instanceGroup.metadata) {
                if (instanceMetaDataJson.publicIp != null) {
                    list.add(instanceMetaDataJson.publicIp)
                }
            }
            returnValues.put(instanceGroup.group, list)
        }
        if (everyGroupDataNeeded(group)) {
            return returnValues
        }
        return object : HashMap<String, List<String>>() {
            init {
                put(group, returnValues[group])
            }
        }
    }

    private fun everyGroupDataNeeded(group: String?): Boolean {
        return group == null || "" == group
    }

    private fun validateAvailabilityZone(region: StackRegion, availabilityZone: StackAvailabilityZone?) {
        val zonesByRegion = shellContext.getAvailabilityZonesByRegion(shellContext.activeCloudPlatform, region.name)
        if (availabilityZone != null && zonesByRegion != null && !zonesByRegion.contains(availabilityZone.name)) {
            throw ValidationException("Availability zone is not in the selected region. The available zones in the regions are: " + zonesByRegion)
        }
    }

    private fun validateNetwork() {
        val networkId = shellContext.activeNetworkId
        if (networkId == null || networkId != null && shellContext.networksByProvider[networkId] === shellContext.activeCloudPlatform) {
            throw ValidationException("A network must be selected with the same cloud platform as the credential!")
        }
    }

    private fun validateSecurityGroup() {
        val securityGroupId = shellContext.activeSecurityGroupId ?: throw ValidationException("A security group must be selected")
    }

    private fun validateRegion(region: StackRegion) {
        val regionsByPlatform = shellContext.getRegionsByPlatform(shellContext.activeCloudPlatform)
        if (regionsByPlatform != null && !regionsByPlatform.isEmpty() && !regionsByPlatform.contains(region.name)) {
            throw ValidationException("Region is not available for the selected platform.")
        }
    }

    private fun validateInstanceGroups() {
        shellContext.instanceGroups.values.stream().filter({ i -> "GATEWAY" == i.type }).findAny().orElseThrow({ ValidationException("You must specify where to install ambari server to with '--ambariServer true' on instancegroup") })
    }

    private fun prepareCluster(stackId: String) {
        try {
            val cluster = shellContext.cloudbreakClient().clusterEndpoint()[java.lang.Long.valueOf(stackId)]
            if (cluster != null) {
                val blueprintId = cluster.blueprintId!!.toString()
                shellContext.addBlueprint(blueprintId)
            }
        } catch (e: Exception) {
            return
        }

    }

}
