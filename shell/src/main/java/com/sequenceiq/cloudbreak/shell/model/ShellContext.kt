package com.sequenceiq.cloudbreak.shell.model

import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.Lists
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse
import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateResponse
import com.sequenceiq.cloudbreak.api.model.CredentialResponse
import com.sequenceiq.cloudbreak.api.model.FileSystemType
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson
import com.sequenceiq.cloudbreak.api.model.StackResponse
import com.sequenceiq.cloudbreak.api.model.TemplateResponse
import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.cloudbreak.shell.commands.provider.AzureCommands
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer
import com.sequenceiq.cloudbreak.shell.transformer.OutputTransformer
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer

@Component
class ShellContext {
    private var platformToVariants: Map<String, Collection<String>>? = null
    private var regions: Map<String, Collection<String>>? = null
    private var availabilityZones: Map<String, Map<String, Collection<String>>>? = null
    private var volumeTypes: Map<String, Collection<String>>? = null
    private var instanceTypes: Map<String, List<Map<String, String>>>? = null
    private var orchestrators: Map<String, Collection<String>>? = null
    private var focus: Focus? = null
    private var hint: Hints? = null
    private val properties = HashMap<PropertyKey, String>()
    private var instanceGroups: MutableMap<String, InstanceGroupEntry> = HashMap()
    private var hostGroups: MutableMap<String, HostgroupEntry> = HashMap()
    private var activeHostGroups: MutableSet<String> = HashSet()
    private var activeInstanceGroups: MutableSet<String> = HashSet()
    private val activeTemplates = HashSet<String>()
    private val activeTemplateNames = HashSet<String>()
    private var activeCloudPlatform: String? = null
    private val networksByProvider = HashMap<Long, String>()
    var securityGroups: MutableMap<Long, String> = HashMap()
        get() = securityGroups
    var activeNetworkId: Long? = null
    var activeSecurityGroupId: Long? = null
    var fileSystemType: FileSystemType? = null
    var fileSystemParameters: Map<String, Any> = HashMap()
    var defaultFileSystem: Boolean? = null
    var selectedMarathonStackId: Long? = null
    var selectedMarathonStackName: String? = null
    private var constraintTemplates: MutableSet<String> = HashSet()
    private var marathonHostgroups: MutableMap<String, MarathonHostgroupEntry> = HashMap()

    @Inject
    private val cloudbreakClient: CloudbreakClient? = null

    @Inject
    private val responseTransformer: ResponseTransformer<Collection<Any>>? = null

    @Inject
    private val exceptionTransformer: ExceptionTransformer? = null

    @Inject
    private val objectMapper: ObjectMapper? = null

    @Inject
    private val outputTransformer: OutputTransformer? = null

    init {
        this.focus = rootFocus
        this.hint = Hints.NONE
        this.instanceGroups = HashMap<String, InstanceGroupEntry>()
        this.hostGroups = HashMap<String, HostgroupEntry>()
        this.activeHostGroups = HashSet<String>()
        this.activeInstanceGroups = HashSet<String>()
        constraintTemplates = HashSet<String>()
        marathonHostgroups = HashMap<String, MarathonHostgroupEntry>()
    }

    fun responseTransformer(): ResponseTransformer<Collection<Any>> {
        return responseTransformer
    }

    fun exceptionTransformer(): ExceptionTransformer {
        return exceptionTransformer
    }

    fun cloudbreakClient(): CloudbreakClient {
        return cloudbreakClient
    }

    fun outputTransformer(): OutputTransformer {
        return outputTransformer
    }

    fun objectMapper(): ObjectMapper {
        return objectMapper
    }

    val isStackAvailable: Boolean
        get() = isPropertyAvailable(PropertyKey.STACK_ID)

    fun addStack(id: String, name: String) {
        addProperty(PropertyKey.STACK_ID, id)
        addProperty(PropertyKey.STACK_NAME, name)
        setStackAccessible()
    }

    fun resetFileSystemConfiguration() {
        this.fileSystemParameters = HashMap<String, Any>()
        this.defaultFileSystem = null
        this.defaultFileSystem = null
    }

    fun getActiveCloudPlatform(): String {
        return if (this.activeCloudPlatform == null) "" else this.activeCloudPlatform
    }

    val isAzureActiveCredential: Boolean
        get() = AzureCommands.PLATFORM == getActiveCloudPlatform()

    fun removeStack(id: String) {
        removeProperty(PropertyKey.STACK_ID, id)
    }

    fun getInstanceGroups(): Map<String, InstanceGroupEntry> {
        return this.instanceGroups
    }

    fun getHostGroups(): Map<String, HostgroupEntry> {
        return hostGroups
    }

    fun putInstanceGroup(name: String, value: InstanceGroupEntry): Map<String, InstanceGroupEntry> {
        this.instanceGroups.put(name, value)
        return this.instanceGroups
    }

    fun putHostGroup(name: String, hostgroupEntry: HostgroupEntry): Map<String, HostgroupEntry> {
        this.hostGroups.put(name, hostgroupEntry)
        return this.hostGroups
    }

    fun getActiveTemplates(): Set<String> {
        return activeTemplates
    }

    fun getActiveTemplateNames(): Set<String> {
        return activeTemplateNames
    }

    val isBlueprintAvailable: Boolean
        get() = isPropertyAvailable(PropertyKey.BLUEPRINT_ID)

    @Throws(Exception::class)
    fun addBlueprint(id: String) {
        this.instanceGroups = HashMap<String, InstanceGroupEntry>()
        this.hostGroups = HashMap<String, HostgroupEntry>()
        this.activeInstanceGroups = HashSet<String>()
        this.activeHostGroups = HashSet<String>()
        val blueprintText = getBlueprintText(id)
        val hostGroups = objectMapper!!.readTree(blueprintText.toByteArray()).get("host_groups")
        for (hostGroup in hostGroups) {
            activeHostGroups.add(hostGroup.get("name").asText())
            activeInstanceGroups.add(hostGroup.get("name").asText())
        }
        addProperty(PropertyKey.BLUEPRINT_ID, id)
        setBlueprintAccessible()
    }

    val blueprintText: String
        get() = getBlueprintText(blueprintId)

    fun prepareInstanceGroups(stack: StackResponse) {
        this.instanceGroups = HashMap<String, InstanceGroupEntry>()
        this.activeInstanceGroups = HashSet<String>()
        for (instanceGroup in stack.instanceGroups) {
            this.activeInstanceGroups.add(instanceGroup.group)
            instanceGroups.put(
                    instanceGroup.group,
                    InstanceGroupEntry(
                            java.lang.Long.valueOf(instanceGroup.templateId!!),
                            Integer.valueOf(instanceGroup.nodeCount),
                            instanceGroup.type.name))
        }
    }

    val isSelectedMarathonStackAvailable: Boolean
        get() = selectedMarathonStackId != null

    fun resetSelectedMarathonStackId() {
        selectedMarathonStackId = null
    }

    fun resetMarathonHostGroups() {
        this.marathonHostgroups = HashMap<String, MarathonHostgroupEntry>()
    }

    val constraints: Set<String>
        get() = constraintTemplates

    fun setConstraints(constraintTemplateResponses: Set<ConstraintTemplateResponse>) {
        constraintTemplates = HashSet<String>()
        for (constraintTemplateResponse in constraintTemplateResponses) {
            constraintTemplates.add(constraintTemplateResponse.name)
        }
    }

    fun putMarathonHostGroup(name: String, hostgroupEntry: MarathonHostgroupEntry): Map<String, MarathonHostgroupEntry> {
        this.marathonHostgroups.put(name, hostgroupEntry)
        return this.marathonHostgroups
    }

    val marathonHostGroups: Map<String, MarathonHostgroupEntry>
        get() = marathonHostgroups

    val isCredentialAvailable: Boolean
        get() = isPropertyAvailable(PropertyKey.CREDENTIAL_ID)

    @Throws(Exception::class)
    fun setCredential(id: String) {
        val credential = cloudbreakClient!!.credentialEndpoint()[java.lang.Long.valueOf(id)]
        this.activeCloudPlatform = credential.cloudPlatform
        val templateResponses = ArrayList<TemplateResponse>()
        for (templateResponse in cloudbreakClient.templateEndpoint().publics) {
            if (this.activeCloudPlatform == templateResponse.cloudPlatform) {
                templateResponses.add(templateResponse)
            }
        }
        fillTemplates(templateResponses)
        addProperty(PropertyKey.CREDENTIAL_ID, id)
        setCredentialAccessible()
    }

    private fun fillTemplates(templateList: List<TemplateResponse>) {
        for (t in templateList) {
            this.activeTemplateNames.add(t.name)
            this.activeTemplates.add(t.id!!.toString())
        }
    }

    fun setPlatformToVariantsMap(platformToVariants: Map<String, Collection<String>>) {
        this.platformToVariants = platformToVariants
    }

    fun getVariantsByPlatform(platform: String): Collection<String> {
        return platformToVariants!![platform]
    }

    fun setRegions(regions: Map<String, Collection<String>>) {
        this.regions = regions
    }

    fun getRegionsByPlatform(platform: String): Collection<String> {
        return regions!![platform]
    }

    fun setAvailabilityZones(availabilityZones: Map<String, Map<String, Collection<String>>>) {
        this.availabilityZones = availabilityZones
    }

    fun getAvailabilityZonesByPlatform(platform: String): Collection<String> {
        val result = Lists.newArrayList<String>()
        val regionZones = availabilityZones!![platform]
        for (zones in regionZones.values) {
            result.addAll(zones)
        }
        return result
    }

    fun getAvailabilityZonesByRegion(platform: String, region: String): Collection<String> {
        return availabilityZones!![platform].get(region)
    }

    fun getInstanceTypeNamesByPlatform(platform: String): Collection<String> {
        val result = Lists.newArrayList<String>()
        val platformInstances = instanceTypes!![platform]
        for (instance in platformInstances) {
            result.add(instance.get("value"))
        }
        return result
    }

    fun getOrchestratorNamesByPlatform(platform: String): Collection<String> {
        val result = Lists.newArrayList<String>()
        return orchestrators!![platform]
    }

    fun getActiveHostGroups(): Set<String> {
        return activeHostGroups
    }

    fun getActiveInstanceGroups(): Set<String> {
        return activeInstanceGroups
    }

    fun setBlueprintAccessible() {
        addProperty(PropertyKey.BLUEPRINT_ACCESSIBLE, ACCESSIBLE)
    }

    val isBlueprintAccessible: Boolean
        get() = isPropertyAvailable(PropertyKey.BLUEPRINT_ACCESSIBLE)

    fun setCredentialAccessible() {
        addProperty(PropertyKey.CREDENTIAL_ACCESSIBLE, ACCESSIBLE)
    }

    val isCredentialAccessible: Boolean
        get() = isPropertyAvailable(PropertyKey.CREDENTIAL_ACCESSIBLE)

    fun setStackAccessible() {
        addProperty(PropertyKey.STACK_ACCESSIBLE, ACCESSIBLE)
    }

    val isStackAccessible: Boolean
        get() = isPropertyAvailable(PropertyKey.STACK_ACCESSIBLE)

    fun setRecipeAccessible() {
        addProperty(PropertyKey.RECIPE_ACCESSIBLE, ACCESSIBLE)
    }

    val isRecipeAccessible: Boolean
        get() = isPropertyAvailable(PropertyKey.RECIPE_ACCESSIBLE)

    val stackId: String
        get() = getLastPropertyValue(PropertyKey.STACK_ID)

    val stackName: String
        get() = getLastPropertyValue(PropertyKey.STACK_NAME)

    val blueprintId: String
        get() = getLastPropertyValue(PropertyKey.BLUEPRINT_ID)

    val recipeId: String
        get() = getLastPropertyValue(PropertyKey.RECIPE_ID)

    val credentialId: String
        get() = getLastPropertyValue(PropertyKey.CREDENTIAL_ID)

    fun getNetworksByProvider(): Map<Long, String> {
        return networksByProvider
    }

    val isSssdConfigAccessible: Boolean
        get() = isPropertyAvailable(PropertyKey.SSSDCONFIG_ACCESSIBLE)

    fun setSssdConfigAccessible() {
        addProperty(PropertyKey.SSSDCONFIG_ACCESSIBLE, ACCESSIBLE)
    }

    @Throws(Exception::class)
    fun addSssdConfig(id: String) {
        addProperty(PropertyKey.SSSDCONFIG_ID, id)
        setSssdConfigAccessible()
    }

    val sssdConfigId: String
        get() = getLastPropertyValue(PropertyKey.SSSDCONFIG_ID)

    fun putNetwork(id: Long?, provider: String) {
        networksByProvider.put(id, provider)
    }

    fun putNetworks(networksByProvider: Map<Long, String>) {
        this.networksByProvider.putAll(networksByProvider)
    }

    fun putSecurityGroup(id: Long?, name: String) {
        this.securityGroups.put(id, name)
    }

    /**
     * Sets the focus to the root.
     */
    fun resetFocus() {
        this.focus = rootFocus
    }

    /**
     * Sets the focus.

     * @param id   target of the focus
     * *
     * @param type type of the focus
     */
    fun setFocus(id: String, type: FocusType) {
        this.focus = Focus(id, type)
    }

    val focusType: FocusType
        get() = focus!!.type

    /**
     * Sets what should be the next hint message.

     * @param hint the new message
     */
    fun setHint(hint: Hints) {
        this.hint = hint
    }

    /**
     * Returns the context sensitive prompt.

     * @return text of the prompt
     */
    val prompt: String
        get() = if (focus!!.isType(FocusType.ROOT)) "cloudbreak-shell>" else formatPrompt(focus!!.prefix, focus!!.value)

    /**
     * Returns some context sensitive hint.

     * @return hint
     */
    val hint: String
        get() = "Hint: " + hint!!.message()

    private val rootFocus: Focus
        get() = Focus("root", FocusType.ROOT)

    private fun formatPrompt(prefix: String, postfix: String?): String {
        return prefix + (if (postfix == null) "" else ":" + postfix) + ">"
    }

    private fun isPropertyAvailable(key: PropertyKey): Boolean {
        return properties[key] != null && !properties[key].isEmpty()
    }

    val isMarathonMode: Boolean
        get() = focusType == FocusType.MARATHON

    private fun addProperty(key: PropertyKey, value: String) {
        properties.remove(key)
        properties.put(key, value)
    }

    private fun removeProperty(key: PropertyKey, value: String) {
        properties.remove(key)
    }

    private fun getLastPropertyValue(key: PropertyKey): String {
        try {
            return properties[key]
        } catch (ex: Exception) {
            return ""
        }

    }

    private fun getBlueprintText(id: String): String {
        val bp = cloudbreakClient!!.blueprintEndpoint()[java.lang.Long.valueOf(id)]
        return bp.ambariBlueprint
    }

    fun setVolumeTypes(volumeTypes: Map<String, Collection<String>>) {
        this.volumeTypes = volumeTypes
    }

    fun getVolumeTypesByPlatform(platform: String): Collection<String> {
        return volumeTypes!![platform]
    }

    fun setInstanceTypes(instanceTypes: Map<String, List<Map<String, String>>>) {
        this.instanceTypes = instanceTypes
    }

    fun setOrchestrators(orchestrators: Map<String, Collection<String>>) {
        this.orchestrators = orchestrators
    }

    private enum class PropertyKey {
        CREDENTIAL_ID,
        BLUEPRINT_ID,
        RECIPE_ID,
        STACK_ID,
        STACK_NAME,
        CREDENTIAL_ACCESSIBLE,
        BLUEPRINT_ACCESSIBLE,
        STACK_ACCESSIBLE,
        RECIPE_ACCESSIBLE,
        SSSDCONFIG_ACCESSIBLE,
        SSSDCONFIG_ID
    }

    companion object {

        private val ACCESSIBLE = "accessible"
    }
}