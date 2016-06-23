package com.sequenceiq.cloudbreak.controller.validation.blueprint

import java.io.IOException
import java.util.ArrayList
import java.util.stream.Collectors

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.collect.Sets
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.domain.Constraint
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.InstanceGroup

@RunWith(MockitoJUnitRunner::class)
class BlueprintValidatorTest {

    @Mock
    private val stackServiceComponentDescriptors: StackServiceComponentDescriptors? = null
    @Mock
    private val objectMapper: ObjectMapper? = null
    @InjectMocks
    private val underTest: BlueprintValidator? = null

    @Before
    fun setUp() {
        setupStackServiceComponentDescriptors()
    }

    @Test(expected = BadRequestException::class)
    @Throws(Exception::class)
    fun testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenJsonTreeCreationIsFailed() {
        // GIVEN
        val blueprint = createBlueprint()
        val instanceGroups = createInstanceGroups()
        val hostGroups = createHostGroups(instanceGroups)
        BDDMockito.given(objectMapper!!.readTree(BDDMockito.anyString())).willThrow(IOException())
        // WHEN
        underTest!!.validateBlueprintForStack(blueprint, hostGroups, instanceGroups)
        // THEN throw exception
    }

    @Test(expected = BadRequestException::class)
    @Throws(IOException::class)
    fun testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenNoInstanceGroupForAHostGroup() {
        // GIVEN
        val blueprint = createBlueprint()
        val instanceGroups = createInstanceGroups()
        val hostGroups = createHostGroups(instanceGroups.stream().limit((instanceGroups.size - 1).toLong()).collect(Collectors.toSet<InstanceGroup>()))
        val blueprintJsonTree = createJsonTree()
        BDDMockito.given(objectMapper!!.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree)
        // WHEN
        underTest!!.validateBlueprintForStack(blueprint, hostGroups, instanceGroups)
        // THEN throw exception
    }

    @Test(expected = BadRequestException::class)
    @Throws(IOException::class)
    fun testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenNodeCountForAHostGroupIsMoreThanMax() {
        // GIVEN
        val blueprint = createBlueprint()
        val instanceGroups = createInstanceGroups()
        val hostGroups = createHostGroups(instanceGroups)
        instanceGroups.add(createInstanceGroup("gateway", 1))
        val blueprintJsonTree = createJsonTreeWithIllegalGroup()
        BDDMockito.given(objectMapper!!.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree)
        // WHEN
        underTest!!.validateBlueprintForStack(blueprint, hostGroups, instanceGroups)
        // THEN throw exception
    }

    @Test(expected = BadRequestException::class)
    @Throws(IOException::class)
    fun testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenGroupCountsAreDifferent() {
        // GIVEN
        val blueprint = createBlueprint()
        val instanceGroups = createInstanceGroups()
        val hostGroups = createHostGroups(instanceGroups)
        instanceGroups.add(createInstanceGroup("gateway", 1))
        val blueprintJsonTree = createJsonTreeWithTooMuchGroup()
        BDDMockito.given(objectMapper!!.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree)
        // WHEN
        underTest!!.validateBlueprintForStack(blueprint, hostGroups, instanceGroups)
        // THEN throw exception
    }

    @Test(expected = BadRequestException::class)
    @Throws(IOException::class)
    fun testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenNotEnoughGroupDefinedInBlueprint() {
        // GIVEN
        val blueprint = createBlueprint()
        val instanceGroups = createInstanceGroups()
        val hostGroups = createHostGroups(instanceGroups)
        val blueprintJsonTree = createJsonTreeWithNotEnoughGroup()
        BDDMockito.given(objectMapper!!.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree)
        // WHEN
        underTest!!.validateBlueprintForStack(blueprint, hostGroups, instanceGroups)
        // THEN throw exception
    }

    @Test(expected = BadRequestException::class)
    @Throws(IOException::class)
    fun testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenComponentIsInMoreGroupsAndNodeCountIsMoreThanMax() {
        // GIVEN
        val blueprint = createBlueprint()
        val instanceGroups = createInstanceGroups()
        val hostGroups = createHostGroups(instanceGroups)
        instanceGroups.add(createInstanceGroup("gateway", 1))
        val blueprintJsonTree = createJsonTreeWithComponentInMoreGroups()
        BDDMockito.given(objectMapper!!.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree)
        // WHEN
        underTest!!.validateBlueprintForStack(blueprint, hostGroups, instanceGroups)
        // THEN throw exception
    }

    @Test(expected = BadRequestException::class)
    @Throws(IOException::class)
    fun testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenComponentIsLessThanMin() {
        // GIVEN
        val blueprint = createBlueprint()
        val instanceGroups = createInstanceGroups()
        val hostGroups = createHostGroups(instanceGroups)
        instanceGroups.add(createInstanceGroup("gateway", 1))
        val blueprintJsonTree = createJsonTreeWithComponentIsLess()
        BDDMockito.given(objectMapper!!.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree)
        // WHEN
        underTest!!.validateBlueprintForStack(blueprint, hostGroups, instanceGroups)
        // THEN throw exception
    }

    @Test
    @Throws(IOException::class)
    fun testValidateBlueprintForStackShouldNotThrowAnyExceptionWhenBlueprintIsValid() {
        // GIVEN
        val blueprint = createBlueprint()
        val instanceGroups = createInstanceGroups()
        val hostGroups = createHostGroups(instanceGroups)
        instanceGroups.add(createInstanceGroup("gateway", 1))
        val blueprintJsonTree = createJsonTree()
        BDDMockito.given(objectMapper!!.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree)
        // WHEN
        underTest!!.validateBlueprintForStack(blueprint, hostGroups, instanceGroups)
        // THEN doesn't throw exception
    }

    @Test
    @Throws(IOException::class)
    fun testValidateBlueprintForStackShouldNotThrowAnyExceptionWhenBlueprintContainsUnknownComponent() {
        // GIVEN
        val blueprint = createBlueprint()
        val instanceGroups = createInstanceGroups()
        val hostGroups = createHostGroups(instanceGroups)
        instanceGroups.add(createInstanceGroup("gateway", 1))
        val blueprintJsonTree = createJsonTreeWithUnknownComponent()
        BDDMockito.given(objectMapper!!.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree)
        // WHEN
        underTest!!.validateBlueprintForStack(blueprint, hostGroups, instanceGroups)
        // THEN doesn't throw exception
    }

    @Test(expected = BadRequestException::class)
    @Throws(IOException::class)
    fun testHostGroupScalingThrowsBadRequestExceptionWhenNodeCountIsMoreThanMax() {
        // GIVEN
        val blueprint = createBlueprint()
        val blueprintJsonTree = createJsonTree()
        val instanceGroup = createInstanceGroup(GROUP3, 3)
        val hostGroup = createHostGroup(instanceGroup.groupName, instanceGroup)
        BDDMockito.given(objectMapper!!.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree)
        // WHEN
        underTest!!.validateHostGroupScalingRequest(blueprint, hostGroup, 1)
        // THEN throw exception
    }

    @Test(expected = BadRequestException::class)
    @Throws(IOException::class)
    fun testHostGroupScalingThrowsBadRequestExceptionWhenNodeCountIsLessThanMin() {
        // GIVEN
        val blueprint = createBlueprint()
        val blueprintJsonTree = createJsonTree()
        val instanceGroup = createInstanceGroup(GROUP3, 1)
        val hostGroup = createHostGroup(instanceGroup.groupName, instanceGroup)
        BDDMockito.given(objectMapper!!.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree)
        // WHEN
        underTest!!.validateHostGroupScalingRequest(blueprint, hostGroup, -1)
        // THEN throw exception
    }

    @Test
    @Throws(IOException::class)
    fun testHostGroupScalingNoThrowsAnyExceptionWhenNumbersAreOk() {
        // GIVEN
        val blueprint = createBlueprint()
        val blueprintJsonTree = createJsonTree()
        val instanceGroup = createInstanceGroup(GROUP3, 2)
        val hostGroup = createHostGroup(instanceGroup.groupName, instanceGroup)
        BDDMockito.given(objectMapper!!.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree)
        // WHEN
        underTest!!.validateHostGroupScalingRequest(blueprint, hostGroup, 1)
        // THEN throw exception
    }

    private fun setupStackServiceComponentDescriptors() {
        BDDMockito.given(stackServiceComponentDescriptors!!.get(MA_MIN1_MAX5)).willReturn(StackServiceComponentDescriptor(MA_MIN1_MAX5, "MASTER", 1, 5))
        BDDMockito.given(stackServiceComponentDescriptors.get(MA_MIN1_MAX1)).willReturn(StackServiceComponentDescriptor(MA_MIN1_MAX1, "MASTER", 1, 1))
        BDDMockito.given(stackServiceComponentDescriptors.get(MA_MIN1_MAX3)).willReturn(StackServiceComponentDescriptor(MA_MIN1_MAX3, "MASTER", 1, 3))
        BDDMockito.given(stackServiceComponentDescriptors.get(SL_MIN0_MAX3)).willReturn(StackServiceComponentDescriptor(SL_MIN0_MAX3, "SLAVE", 0, 3))
        BDDMockito.given(stackServiceComponentDescriptors.get(SL_MIN5_MAX6)).willReturn(StackServiceComponentDescriptor(SL_MIN5_MAX6, "SLAVE", 5, 6))
    }

    private fun createBlueprint(): Blueprint {
        val blueprint = Blueprint()
        blueprint.blueprintText = BLUEPRINT_STRING
        return blueprint
    }

    private fun createInstanceGroups(): MutableSet<InstanceGroup> {
        val groups = Sets.newHashSet<InstanceGroup>()
        groups.add(createInstanceGroup(GROUP1, 1))
        groups.add(createInstanceGroup(GROUP2, 2))
        groups.add(createInstanceGroup(GROUP3, 3))
        return groups
    }

    private fun createInstanceGroup(groupName: String, nodeCount: Int): InstanceGroup {
        val group = InstanceGroup()
        group.groupName = groupName
        group.nodeCount = nodeCount
        return group
    }

    private fun createHostGroups(instanceGroups: Set<InstanceGroup>): Set<HostGroup> {
        val groups = Sets.newHashSet<HostGroup>()
        for (instanceGroup in ArrayList(instanceGroups)) {
            groups.add(createHostGroup(instanceGroup.groupName, instanceGroup))
        }
        return groups
    }

    private fun createHostGroup(groupName: String, instanceGroup: InstanceGroup): HostGroup {
        val group = HostGroup()
        group.name = groupName
        val constraint = Constraint()
        constraint.hostCount = instanceGroup.nodeCount
        constraint.instanceGroup = instanceGroup
        group.constraint = constraint
        return group
    }

    private fun createJsonTreeWithIllegalGroup(): JsonNode {
        val jsonNodeFactory = JsonNodeFactory.instance
        val rootNode = jsonNodeFactory.objectNode()
        val hostGroupsNode = rootNode.putArray("host_groups")
        addHostGroup(hostGroupsNode, GROUP1, SL_MIN0_MAX3)
        addHostGroup(hostGroupsNode, GROUP2, MA_MIN1_MAX5, MA_MIN1_MAX1)
        addHostGroup(hostGroupsNode, GROUP3, MA_MIN1_MAX3)
        return rootNode
    }

    private fun createJsonTreeWithTooMuchGroup(): JsonNode {
        val jsonNodeFactory = JsonNodeFactory.instance
        val rootNode = jsonNodeFactory.objectNode()
        val hostGroupsNode = rootNode.putArray("host_groups")
        addHostGroup(hostGroupsNode, GROUP1, MA_MIN1_MAX1)
        addHostGroup(hostGroupsNode, GROUP2, MA_MIN1_MAX5)
        addHostGroup(hostGroupsNode, GROUP3, MA_MIN1_MAX3)
        addHostGroup(hostGroupsNode, GROUP4, SL_MIN0_MAX3)
        return rootNode
    }

    private fun createJsonTreeWithNotEnoughGroup(): JsonNode {
        val jsonNodeFactory = JsonNodeFactory.instance
        val rootNode = jsonNodeFactory.objectNode()
        val hostGroupsNode = rootNode.putArray("host_groups")
        addHostGroup(hostGroupsNode, GROUP1, MA_MIN1_MAX1)
        addHostGroup(hostGroupsNode, GROUP2, MA_MIN1_MAX5)
        return rootNode
    }

    private fun createJsonTreeWithUnknownComponent(): JsonNode {
        val jsonNodeFactory = JsonNodeFactory.instance
        val rootNode = jsonNodeFactory.objectNode()
        val hostGroupsNode = rootNode.putArray("host_groups")
        addHostGroup(hostGroupsNode, GROUP1, MA_MIN1_MAX1)
        addHostGroup(hostGroupsNode, GROUP2, MA_MIN1_MAX5)
        addHostGroup(hostGroupsNode, GROUP3, UNKNOWN)
        return rootNode
    }

    private fun createJsonTreeWithComponentInMoreGroups(): JsonNode {
        val jsonNodeFactory = JsonNodeFactory.instance
        val rootNode = jsonNodeFactory.objectNode()
        val hostGroupsNode = rootNode.putArray("host_groups")
        addHostGroup(hostGroupsNode, GROUP1, MA_MIN1_MAX1, MA_MIN1_MAX3)
        addHostGroup(hostGroupsNode, GROUP2, MA_MIN1_MAX5)
        addHostGroup(hostGroupsNode, GROUP3, MA_MIN1_MAX3)
        return rootNode
    }

    private fun createJsonTreeWithComponentIsLess(): JsonNode {
        val jsonNodeFactory = JsonNodeFactory.instance
        val rootNode = jsonNodeFactory.objectNode()
        val hostGroupsNode = rootNode.putArray("host_groups")
        addHostGroup(hostGroupsNode, GROUP1, MA_MIN1_MAX1)
        addHostGroup(hostGroupsNode, GROUP2, MA_MIN1_MAX3)
        addHostGroup(hostGroupsNode, GROUP3, SL_MIN5_MAX6)
        return rootNode
    }

    private fun createJsonTree(): JsonNode {
        val jsonNodeFactory = JsonNodeFactory.instance
        val rootNode = jsonNodeFactory.objectNode()
        val hostGroupsNode = rootNode.putArray("host_groups")
        addHostGroup(hostGroupsNode, GROUP1, SL_MIN0_MAX3, MA_MIN1_MAX1)
        addHostGroup(hostGroupsNode, GROUP2, MA_MIN1_MAX5)
        addHostGroup(hostGroupsNode, GROUP3, MA_MIN1_MAX3)
        return rootNode
    }

    private fun addHostGroup(hostGroupsNode: ArrayNode, name: String, vararg components: String) {
        val hostGroupNode = hostGroupsNode.addObject()
        hostGroupNode.put("name", name)
        val componentsNode = hostGroupNode.putArray("components")
        for (comp in components) {
            val compNode = componentsNode.addObject()
            compNode.put("name", comp)
        }
    }

    companion object {
        private val BLUEPRINT_STRING = "blueprint"
        private val GROUP1 = "group1"
        private val GROUP2 = "group2"
        private val GROUP3 = "group3"
        private val GROUP4 = "group4"
        private val MA_MIN1_MAX5 = "mastercomp1"
        private val MA_MIN1_MAX1 = "mastercomp2"
        private val MA_MIN1_MAX3 = "mastercomp3"
        private val SL_MIN0_MAX3 = "slavecomp1"
        private val SL_MIN5_MAX6 = "slavecomp2"
        private val UNKNOWN = "unknown"
    }
}
