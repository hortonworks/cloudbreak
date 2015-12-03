package com.sequenceiq.cloudbreak.controller.validation.blueprint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintValidatorTest {
    private static final String BLUEPRINT_STRING = "blueprint";
    private static final String GROUP1 = "group1";
    private static final String GROUP2 = "group2";
    private static final String GROUP3 = "group3";
    private static final String GROUP4 = "group4";
    private static final String MA_MIN1_MAX5 = "mastercomp1";
    private static final String MA_MIN1_MAX1 = "mastercomp2";
    private static final String MA_MIN1_MAX3 = "mastercomp3";
    private static final String SL_MIN0_MAX3 = "slavecomp1";
    private static final String SL_MIN5_MAX6 = "slavecomp2";
    private static final String UNKNOWN = "unknown";

    @Mock
    private StackServiceComponentDescriptors stackServiceComponentDescriptors;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private BlueprintValidator underTest;

    @Before
    public void setUp() {
        setupStackServiceComponentDescriptors();
    }

    @Test(expected = BadRequestException.class)
    public void testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenJsonTreeCreationIsFailed() throws Exception {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);
        BDDMockito.given(objectMapper.readTree(BDDMockito.anyString())).willThrow(new IOException());
        // WHEN
        underTest.validateBlueprintForStack(blueprint, hostGroups, instanceGroups);
        // THEN throw exception
    }

    @Test(expected = BadRequestException.class)
    public void testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenNoInstanceGroupForAHostGroup() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);
        JsonNode blueprintJsonTree = createJsonTree();
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        // WHEN
        underTest.validateBlueprintForStack(blueprint, hostGroups, instanceGroups);
        // THEN throw exception
    }

    @Test(expected = BadRequestException.class)
    public void testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenNodeCountForAHostGroupIsMoreThanMax() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);
        instanceGroups.add(createInstanceGroup("gateway", 1));
        JsonNode blueprintJsonTree = createJsonTreeWithIllegalGroup();
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        // WHEN
        underTest.validateBlueprintForStack(blueprint, hostGroups, instanceGroups);
        // THEN throw exception
    }

    @Test(expected = BadRequestException.class)
    public void testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenGroupCountsAreDifferent() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);
        instanceGroups.add(createInstanceGroup("gateway", 1));
        JsonNode blueprintJsonTree = createJsonTreeWithTooMuchGroup();
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        // WHEN
        underTest.validateBlueprintForStack(blueprint, hostGroups, instanceGroups);
        // THEN throw exception
    }

    @Test(expected = BadRequestException.class)
    public void testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenNotEnoughGroupDefinedInBlueprint() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);
        JsonNode blueprintJsonTree = createJsonTreeWithNotEnoughGroup();
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        // WHEN
        underTest.validateBlueprintForStack(blueprint, hostGroups, instanceGroups);
        // THEN throw exception
    }

    @Test(expected = BadRequestException.class)
    public void testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenComponentIsInMoreGroupsAndNodeCountIsMoreThanMax() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);
        instanceGroups.add(createInstanceGroup("gateway", 1));
        JsonNode blueprintJsonTree = createJsonTreeWithComponentInMoreGroups();
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        // WHEN
        underTest.validateBlueprintForStack(blueprint, hostGroups, instanceGroups);
        // THEN throw exception
    }

    @Test(expected = BadRequestException.class)
    public void testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenComponentIsLessThanMin() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);
        instanceGroups.add(createInstanceGroup("gateway", 1));
        JsonNode blueprintJsonTree = createJsonTreeWithComponentIsLess();
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        // WHEN
        underTest.validateBlueprintForStack(blueprint, hostGroups, instanceGroups);
        // THEN throw exception
    }

    @Test
    public void testValidateBlueprintForStackShouldNotThrowAnyExceptionWhenBlueprintIsValid() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);
        instanceGroups.add(createInstanceGroup("gateway", 1));
        JsonNode blueprintJsonTree = createJsonTree();
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        // WHEN
        underTest.validateBlueprintForStack(blueprint, hostGroups, instanceGroups);
        // THEN doesn't throw exception
    }

    @Test
    public void testValidateBlueprintForStackShouldNotThrowAnyExceptionWhenBlueprintContainsUnknownComponent() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);
        instanceGroups.add(createInstanceGroup("gateway", 1));
        JsonNode blueprintJsonTree = createJsonTreeWithUnknownComponent();
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        // WHEN
        underTest.validateBlueprintForStack(blueprint, hostGroups, instanceGroups);
        // THEN doesn't throw exception
    }

    @Test(expected = BadRequestException.class)
    public void testHostGroupScalingThrowsBadRequestExceptionWhenNodeCountIsMoreThanMax() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        JsonNode blueprintJsonTree = createJsonTree();
        InstanceGroup instanceGroup = createInstanceGroup(GROUP3, 3);
        HostGroup hostGroup = createHostGroup(instanceGroup.getGroupName(), instanceGroup);
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        // WHEN
        underTest.validateHostGroupScalingRequest(blueprint, hostGroup, 1);
        // THEN throw exception
    }

    @Test(expected = BadRequestException.class)
    public void testHostGroupScalingThrowsBadRequestExceptionWhenNodeCountIsLessThanMin() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        JsonNode blueprintJsonTree = createJsonTree();
        InstanceGroup instanceGroup = createInstanceGroup(GROUP3, 1);
        HostGroup hostGroup = createHostGroup(instanceGroup.getGroupName(), instanceGroup);
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        // WHEN
        underTest.validateHostGroupScalingRequest(blueprint, hostGroup, -1);
        // THEN throw exception
    }

    @Test
    public void testHostGroupScalingNoThrowsAnyExceptionWhenNumbersAreOk() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        JsonNode blueprintJsonTree = createJsonTree();
        InstanceGroup instanceGroup = createInstanceGroup(GROUP3, 2);
        HostGroup hostGroup = createHostGroup(instanceGroup.getGroupName(), instanceGroup);
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        // WHEN
        underTest.validateHostGroupScalingRequest(blueprint, hostGroup, 1);
        // THEN throw exception
    }

    private void setupStackServiceComponentDescriptors() {
        BDDMockito.given(stackServiceComponentDescriptors.get(MA_MIN1_MAX5)).willReturn(new StackServiceComponentDescriptor(MA_MIN1_MAX5, "MASTER", 1, 5));
        BDDMockito.given(stackServiceComponentDescriptors.get(MA_MIN1_MAX1)).willReturn(new StackServiceComponentDescriptor(MA_MIN1_MAX1, "MASTER", 1, 1));
        BDDMockito.given(stackServiceComponentDescriptors.get(MA_MIN1_MAX3)).willReturn(new StackServiceComponentDescriptor(MA_MIN1_MAX3, "MASTER", 1, 3));
        BDDMockito.given(stackServiceComponentDescriptors.get(SL_MIN0_MAX3)).willReturn(new StackServiceComponentDescriptor(SL_MIN0_MAX3, "SLAVE", 0, 3));
        BDDMockito.given(stackServiceComponentDescriptors.get(SL_MIN5_MAX6)).willReturn(new StackServiceComponentDescriptor(SL_MIN5_MAX6, "SLAVE", 5, 6));
    }

    private Blueprint createBlueprint() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(BLUEPRINT_STRING);
        return blueprint;
    }

    private Set<InstanceGroup> createInstanceGroups() {
        Set<InstanceGroup> groups = Sets.newHashSet();
        groups.add(createInstanceGroup(GROUP1, 1));
        groups.add(createInstanceGroup(GROUP2, 2));
        groups.add(createInstanceGroup(GROUP3, 3));
        return groups;
    }

    private InstanceGroup createInstanceGroup(String groupName, int nodeCount) {
        InstanceGroup group = new InstanceGroup();
        group.setGroupName(groupName);
        group.setNodeCount(nodeCount);
        return group;
    }

    private Set<HostGroup> createHostGroups(Set<InstanceGroup> instanceGroups) {
        Set<HostGroup> groups = Sets.newHashSet();
        for (InstanceGroup instanceGroup : new ArrayList<>(instanceGroups)) {
            groups.add(createHostGroup(instanceGroup.getGroupName(), instanceGroup));
        }
        return groups;
    }

    private HostGroup createHostGroup(String groupName, InstanceGroup instanceGroup) {
        HostGroup group = new HostGroup();
        group.setName(groupName);
        group.setInstanceGroup(instanceGroup);
        return group;
    }

    private JsonNode createJsonTreeWithIllegalGroup() {
        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        ObjectNode rootNode = jsonNodeFactory.objectNode();
        ArrayNode hostGroupsNode = rootNode.putArray("host_groups");
        addHostGroup(hostGroupsNode, GROUP1, SL_MIN0_MAX3);
        addHostGroup(hostGroupsNode, GROUP2, MA_MIN1_MAX5, MA_MIN1_MAX1);
        addHostGroup(hostGroupsNode, GROUP3, MA_MIN1_MAX3);
        return rootNode;
    }

    private JsonNode createJsonTreeWithTooMuchGroup() {
        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        ObjectNode rootNode = jsonNodeFactory.objectNode();
        ArrayNode hostGroupsNode = rootNode.putArray("host_groups");
        addHostGroup(hostGroupsNode, GROUP1, MA_MIN1_MAX1);
        addHostGroup(hostGroupsNode, GROUP2, MA_MIN1_MAX5);
        addHostGroup(hostGroupsNode, GROUP3, MA_MIN1_MAX3);
        addHostGroup(hostGroupsNode, GROUP4, SL_MIN0_MAX3);
        return rootNode;
    }

    private JsonNode createJsonTreeWithNotEnoughGroup() {
        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        ObjectNode rootNode = jsonNodeFactory.objectNode();
        ArrayNode hostGroupsNode = rootNode.putArray("host_groups");
        addHostGroup(hostGroupsNode, GROUP1, MA_MIN1_MAX1);
        addHostGroup(hostGroupsNode, GROUP2, MA_MIN1_MAX5);
        return rootNode;
    }

    private JsonNode createJsonTreeWithUnknownComponent() {
        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        ObjectNode rootNode = jsonNodeFactory.objectNode();
        ArrayNode hostGroupsNode = rootNode.putArray("host_groups");
        addHostGroup(hostGroupsNode, GROUP1, MA_MIN1_MAX1);
        addHostGroup(hostGroupsNode, GROUP2, MA_MIN1_MAX5);
        addHostGroup(hostGroupsNode, GROUP3, UNKNOWN);
        return rootNode;
    }

    private JsonNode createJsonTreeWithComponentInMoreGroups() {
        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        ObjectNode rootNode = jsonNodeFactory.objectNode();
        ArrayNode hostGroupsNode = rootNode.putArray("host_groups");
        addHostGroup(hostGroupsNode, GROUP1, MA_MIN1_MAX1, MA_MIN1_MAX3);
        addHostGroup(hostGroupsNode, GROUP2, MA_MIN1_MAX5);
        addHostGroup(hostGroupsNode, GROUP3, MA_MIN1_MAX3);
        return rootNode;
    }

    private JsonNode createJsonTreeWithComponentIsLess() {
        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        ObjectNode rootNode = jsonNodeFactory.objectNode();
        ArrayNode hostGroupsNode = rootNode.putArray("host_groups");
        addHostGroup(hostGroupsNode, GROUP1, MA_MIN1_MAX1);
        addHostGroup(hostGroupsNode, GROUP2, MA_MIN1_MAX3);
        addHostGroup(hostGroupsNode, GROUP3, SL_MIN5_MAX6);
        return rootNode;
    }

    private JsonNode createJsonTree() {
        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        ObjectNode rootNode = jsonNodeFactory.objectNode();
        ArrayNode hostGroupsNode = rootNode.putArray("host_groups");
        addHostGroup(hostGroupsNode, GROUP1, SL_MIN0_MAX3, MA_MIN1_MAX1);
        addHostGroup(hostGroupsNode, GROUP2, MA_MIN1_MAX5);
        addHostGroup(hostGroupsNode, GROUP3, MA_MIN1_MAX3);
        return rootNode;
    }

    private void addHostGroup(ArrayNode hostGroupsNode, String name, String... components) {
        ObjectNode hostGroupNode = hostGroupsNode.addObject();
        hostGroupNode.put("name", name);
        ArrayNode componentsNode = hostGroupNode.putArray("components");
        for (String comp : components) {
            ObjectNode compNode = componentsNode.addObject();
            compNode.put("name", comp);
        }
    }
}
