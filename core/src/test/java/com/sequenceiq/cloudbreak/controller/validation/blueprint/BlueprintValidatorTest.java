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
    private static final String COMPONENT1 = "comp1";
    private static final String COMPONENT2 = "comp2";
    private static final String COMPONENT3 = "comp3";
    private static final String SLAVE_COMPONENT = "slavecomp";
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
        JsonNode blueprintJsonTree = createJsonTreeWithUnknownHostGroup();
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
        JsonNode blueprintJsonTree = createJsonTreeWithComponentInMoreGroups();
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

    private void setupStackServiceComponentDescriptors() {
        BDDMockito.given(stackServiceComponentDescriptors.get(COMPONENT1)).willReturn(new StackServiceComponentDescriptor(COMPONENT1, "MASTER", 5));
        BDDMockito.given(stackServiceComponentDescriptors.get(COMPONENT2)).willReturn(new StackServiceComponentDescriptor(COMPONENT2, "MASTER", 1));
        BDDMockito.given(stackServiceComponentDescriptors.get(COMPONENT3)).willReturn(new StackServiceComponentDescriptor(COMPONENT3, "MASTER", 3));
        BDDMockito.given(stackServiceComponentDescriptors.get(SLAVE_COMPONENT)).willReturn(new StackServiceComponentDescriptor(SLAVE_COMPONENT, "SLAVE", 3));
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
        for (InstanceGroup instanceGroup : new ArrayList<InstanceGroup>(instanceGroups)) {
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

    private JsonNode createJsonTreeWithUnknownHostGroup() {
        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        ObjectNode rootNode = jsonNodeFactory.objectNode();
        ArrayNode hostGroupsNode = rootNode.putArray("host_groups");
        addHostGroup(hostGroupsNode, GROUP1, SLAVE_COMPONENT);
        addHostGroup(hostGroupsNode, UNKNOWN);
        return rootNode;
    }

    private JsonNode createJsonTreeWithIllegalGroup() {
        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        ObjectNode rootNode = jsonNodeFactory.objectNode();
        ArrayNode hostGroupsNode = rootNode.putArray("host_groups");
        addHostGroup(hostGroupsNode, GROUP1, SLAVE_COMPONENT);
        addHostGroup(hostGroupsNode, GROUP2, COMPONENT1, COMPONENT2);
        addHostGroup(hostGroupsNode, GROUP3, COMPONENT3);
        return rootNode;
    }

    private JsonNode createJsonTreeWithTooMuchGroup() {
        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        ObjectNode rootNode = jsonNodeFactory.objectNode();
        ArrayNode hostGroupsNode = rootNode.putArray("host_groups");
        addHostGroup(hostGroupsNode, GROUP1, COMPONENT2);
        addHostGroup(hostGroupsNode, GROUP2, COMPONENT1);
        addHostGroup(hostGroupsNode, GROUP3, COMPONENT3);
        addHostGroup(hostGroupsNode, GROUP4, SLAVE_COMPONENT);
        return rootNode;
    }

    private JsonNode createJsonTreeWithNotEnoughGroup() {
        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        ObjectNode rootNode = jsonNodeFactory.objectNode();
        ArrayNode hostGroupsNode = rootNode.putArray("host_groups");
        addHostGroup(hostGroupsNode, GROUP1, COMPONENT2);
        addHostGroup(hostGroupsNode, GROUP2, COMPONENT1);
        return rootNode;
    }

    private JsonNode createJsonTreeWithUnknownComponent() {
        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        ObjectNode rootNode = jsonNodeFactory.objectNode();
        ArrayNode hostGroupsNode = rootNode.putArray("host_groups");
        addHostGroup(hostGroupsNode, GROUP1, COMPONENT2);
        addHostGroup(hostGroupsNode, GROUP2, COMPONENT1);
        addHostGroup(hostGroupsNode, GROUP3, UNKNOWN);
        return rootNode;
    }

    private JsonNode createJsonTreeWithComponentInMoreGroups() {
        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        ObjectNode rootNode = jsonNodeFactory.objectNode();
        ArrayNode hostGroupsNode = rootNode.putArray("host_groups");
        addHostGroup(hostGroupsNode, GROUP1, COMPONENT2, COMPONENT3);
        addHostGroup(hostGroupsNode, GROUP2, COMPONENT1);
        addHostGroup(hostGroupsNode, GROUP3, COMPONENT3);
        return rootNode;
    }

    private JsonNode createJsonTree() {
        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        ObjectNode rootNode = jsonNodeFactory.objectNode();
        ArrayNode hostGroupsNode = rootNode.putArray("host_groups");
        addHostGroup(hostGroupsNode, GROUP1, SLAVE_COMPONENT, COMPONENT2);
        addHostGroup(hostGroupsNode, GROUP2, COMPONENT1);
        addHostGroup(hostGroupsNode, GROUP3, COMPONENT3);
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
