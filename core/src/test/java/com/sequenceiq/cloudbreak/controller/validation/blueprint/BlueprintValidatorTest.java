package com.sequenceiq.cloudbreak.controller.validation.blueprint;

import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.Gateway;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;

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

    private static final String KNOX = "KNOX_GATEWAY";

    private static final String UNKNOWN = "unknown";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

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

    @Test
    public void testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenJsonTreeCreationIsFailed() throws Exception {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);
        IOException expectedException = new IOException("");
        BDDMockito.given(objectMapper.readTree(BDDMockito.anyString())).willThrow(expectedException);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Blueprint [null] can not be parsed from JSON.");
        thrown.expectCause(is(expectedException));
        // WHEN
        underTest.validateBlueprintForStack(blueprint, hostGroups, instanceGroups);
        // THEN throw exception
    }

    @Test
    public void testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenNoInstanceGroupForAHostGroup() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups.stream().sorted().limit(instanceGroups.size() - 1).collect(Collectors.toSet()));
        JsonNode blueprintJsonTree = createJsonTree();
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage(Matchers.startsWith("The host groups in the blueprint"));
        // WHEN
        underTest.validateBlueprintForStack(blueprint, hostGroups, instanceGroups);
        // THEN throw exception
    }

    @Test
    public void testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenNodeCountForAHostGroupIsMoreThanMax() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);
        instanceGroups.add(createInstanceGroup("gateway", 0, 1));
        JsonNode blueprintJsonTree = createJsonTreeWithIllegalGroup();
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("The node count '2' for hostgroup 'group2' cannot be less than '1' or more than '1' because of 'mastercomp2' component");
        // WHEN
        underTest.validateBlueprintForStack(blueprint, hostGroups, instanceGroups);
        // THEN throw exception
    }

    @Test
    public void testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenGroupCountsAreDifferent() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);
        instanceGroups.add(createInstanceGroup("gateway", 0, 1));
        JsonNode blueprintJsonTree = createJsonTreeWithTooMuchGroup();
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage(Matchers.startsWith("The host groups in the blueprint"));
        // WHEN
        underTest.validateBlueprintForStack(blueprint, hostGroups, instanceGroups);
        // THEN throw exception
    }

    @Test
    public void testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenNotEnoughGroupDefinedInBlueprint() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);
        JsonNode blueprintJsonTree = createJsonTreeWithNotEnoughGroup();
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage(Matchers.startsWith("The host groups in the blueprint"));
        // WHEN
        underTest.validateBlueprintForStack(blueprint, hostGroups, instanceGroups);
        // THEN throw exception
    }

    @Test
    public void testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenComponentIsInMoreGroupsAndNodeCountIsMoreThanMax() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);
        instanceGroups.add(createInstanceGroup("gateway", 0, 1));
        JsonNode blueprintJsonTree = createJsonTreeWithComponentInMoreGroups();
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Incorrect number of 'mastercomp3' components are in '[group1, group3]' hostgroups: count: 4, min: 1 max: 3");
        // WHEN
        underTest.validateBlueprintForStack(blueprint, hostGroups, instanceGroups);
        // THEN throw exception
    }

    @Test
    public void testValidateBlueprintForStackShouldThrowBadRequestExceptionWhenComponentIsLessThanMin() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);
        instanceGroups.add(createInstanceGroup("gateway", 0, 1));
        JsonNode blueprintJsonTree = createJsonTreeWithComponentIsLess();
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Incorrect number of 'slavecomp2' components are in '[group3]' hostgroups: count: 3, min: 5 max: 6");
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
        instanceGroups.add(createInstanceGroup("gateway", 0, 1));
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
        instanceGroups.add(createInstanceGroup("gateway", 0, 1));
        JsonNode blueprintJsonTree = createJsonTreeWithUnknownComponent();
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        // WHEN
        underTest.validateBlueprintForStack(blueprint, hostGroups, instanceGroups);
        // THEN doesn't throw exception
    }

    @Test
    public void testHostGroupScalingThrowsBadRequestExceptionWhenNodeCountIsMoreThanMax() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        JsonNode blueprintJsonTree = createJsonTree();
        InstanceGroup instanceGroup = createInstanceGroup(GROUP3, 0, 3);
        HostGroup hostGroup = createHostGroup(instanceGroup.getGroupName(), instanceGroup);
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("The node count '4' for hostgroup 'group3' cannot be less than '1' or more than '3' because of 'mastercomp3' component");
        // WHEN
        underTest.validateHostGroupScalingRequest(blueprint, hostGroup, 1);
        // THEN throw exception
    }

    @Test
    public void testHostGroupScalingThrowsBadRequestExceptionWhenNodeCountIsLessThanMin() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        JsonNode blueprintJsonTree = createJsonTree();
        InstanceGroup instanceGroup = createInstanceGroup(GROUP3, 0, 1);
        HostGroup hostGroup = createHostGroup(instanceGroup.getGroupName(), instanceGroup);
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("The node count '0' for hostgroup 'group3' cannot be less than '1' or more than '3' because of 'mastercomp3' component");
        // WHEN
        underTest.validateHostGroupScalingRequest(blueprint, hostGroup, -1);
        // THEN throw exception
    }

    @Test
    public void testHostGroupScalingNoThrowsAnyExceptionWhenNumbersAreOk() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        JsonNode blueprintJsonTree = createJsonTree();
        InstanceGroup instanceGroup = createInstanceGroup(GROUP3, 0, 2);
        HostGroup hostGroup = createHostGroup(instanceGroup.getGroupName(), instanceGroup);
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        // WHEN
        underTest.validateHostGroupScalingRequest(blueprint, hostGroup, 1);
        // THEN throw exception
    }

    @Test
    public void testKnoxWithoutKerberos() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);
        instanceGroups.add(createInstanceGroup("gateway", 0, 1));
        JsonNode blueprintJsonTree = createJsonTree();
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        Cluster cluster = new Cluster();
        cluster.setSecure(false);

        // WHEN
        underTest.validateBlueprintForStack(cluster, blueprint, hostGroups, instanceGroups);

        // THEN no exception thrown
    }

    @Test
    public void testKerberosWithoutKnox() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);
        instanceGroups.add(createInstanceGroup("gateway", 0, 1));
        JsonNode blueprintJsonTree = createJsonTree();
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        Cluster cluster = new Cluster();
        cluster.setSecure(true);

        // WHEN
        underTest.validateBlueprintForStack(cluster, blueprint, hostGroups, instanceGroups);

        // THEN no exception thrown
    }

    @Test
    public void testKnoxWithNoCluster() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);
        instanceGroups.add(createInstanceGroup("gateway", 0, 1));
        JsonNode blueprintJsonTree = createJsonTree();
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);

        // WHEN
        underTest.validateBlueprintForStack(null, blueprint, hostGroups, instanceGroups);

        // THEN no exception thrown
    }

    @Test
    public void testKnoxValidationWithNoGateway() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);
        instanceGroups.add(createInstanceGroup("gateway", 0, 1));
        JsonNode blueprintJsonTree = createJsonTree();
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(blueprintJsonTree);
        Cluster cluster = new Cluster();
        cluster.setSecure(true);

        // WHEN
        underTest.validateBlueprintForStack(cluster, blueprint, hostGroups, instanceGroups);

        // THEN no exception thrown
    }

    @Test
    public void testKnoxWithKerberosButNoKnoxInTheBlueprintForAllNodes() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        instanceGroups.add(createInstanceGroup("gateway1", 0, 1, InstanceGroupType.GATEWAY));
        instanceGroups.add(createInstanceGroup("gateway2", 1, 1, InstanceGroupType.GATEWAY));
        instanceGroups.add(createInstanceGroup("master", 2, 1, InstanceGroupType.CORE));
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);

        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        ObjectNode rootNode = jsonNodeFactory.objectNode();
        ArrayNode hostGroupsNode = rootNode.putArray("host_groups");
        addHostGroup(hostGroupsNode, "gateway1", SL_MIN0_MAX3, MA_MIN1_MAX1);
        addHostGroup(hostGroupsNode, "gateway2", SL_MIN0_MAX3, MA_MIN1_MAX3);
        addHostGroup(hostGroupsNode, "master", SL_MIN0_MAX3, MA_MIN1_MAX5);
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(rootNode);

        Cluster cluster = new Cluster();
        cluster.setSecure(true);
        Gateway gateway = new Gateway();
        gateway.setEnableGateway(true);
        cluster.setGateway(gateway);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("In case of Knox and Kerberos each 'Ambari Server' node must include the 'KNOX_GATEWAY' service. "
            + "The following host groups are missing the service: gateway1,gateway2");

        // WHEN
        underTest.validateBlueprintForStack(cluster, blueprint, hostGroups, instanceGroups);

        // THEN exception thrown
    }

    @Test
    public void testKnoxWithKerberosButOneNodeMissingKnox() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        instanceGroups.add(createInstanceGroup("gateway1", 0, 1, InstanceGroupType.GATEWAY));
        instanceGroups.add(createInstanceGroup("gateway2", 1, 1, InstanceGroupType.GATEWAY));
        instanceGroups.add(createInstanceGroup("master", 2, 1, InstanceGroupType.CORE));
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);

        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        ObjectNode rootNode = jsonNodeFactory.objectNode();
        ArrayNode hostGroupsNode = rootNode.putArray("host_groups");
        addHostGroup(hostGroupsNode, "gateway1", SL_MIN0_MAX3, KNOX);
        addHostGroup(hostGroupsNode, "gateway2", SL_MIN0_MAX3, MA_MIN1_MAX3);
        addHostGroup(hostGroupsNode, "master", SL_MIN0_MAX3, MA_MIN1_MAX5);
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(rootNode);

        Cluster cluster = new Cluster();
        cluster.setSecure(true);
        Gateway gateway = new Gateway();
        gateway.setEnableGateway(true);
        cluster.setGateway(gateway);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("In case of Knox and Kerberos each 'Ambari Server' node must include the 'KNOX_GATEWAY' service. "
            + "The following host groups are missing the service: gateway2");

        // WHEN
        underTest.validateBlueprintForStack(cluster, blueprint, hostGroups, instanceGroups);

        // THEN exception thrown
    }

    @Test
    public void testKnoxWithKerberosAndNonGwHasKnox() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        instanceGroups.add(createInstanceGroup("gateway1", 0, 1, InstanceGroupType.GATEWAY));
        instanceGroups.add(createInstanceGroup("gateway2", 1, 1, InstanceGroupType.GATEWAY));
        instanceGroups.add(createInstanceGroup("master", 2, 1, InstanceGroupType.CORE));
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);

        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        ObjectNode rootNode = jsonNodeFactory.objectNode();
        ArrayNode hostGroupsNode = rootNode.putArray("host_groups");
        addHostGroup(hostGroupsNode, "gateway1", SL_MIN0_MAX3, MA_MIN1_MAX3);
        addHostGroup(hostGroupsNode, "gateway2", SL_MIN0_MAX3, MA_MIN1_MAX3);
        addHostGroup(hostGroupsNode, "master", SL_MIN0_MAX3, KNOX);
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(rootNode);

        Cluster cluster = new Cluster();
        cluster.setSecure(true);
        Gateway gateway = new Gateway();
        gateway.setEnableGateway(true);
        cluster.setGateway(gateway);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("In case of Knox and Kerberos each 'Ambari Server' node must include the 'KNOX_GATEWAY' service. "
            + "The following host groups are missing the service: gateway1,gateway2");

        // WHEN
        underTest.validateBlueprintForStack(cluster, blueprint, hostGroups, instanceGroups);

        // THEN exception thrown
    }

    @Test
    public void testKnoxWithKerberosAndEachGwHasKnox() throws IOException {
        // GIVEN
        Blueprint blueprint = createBlueprint();
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        instanceGroups.add(createInstanceGroup("gateway1", 0, 1, InstanceGroupType.GATEWAY));
        instanceGroups.add(createInstanceGroup("gateway2", 1, 1, InstanceGroupType.GATEWAY));
        instanceGroups.add(createInstanceGroup("master", 2, 1, InstanceGroupType.CORE));
        Set<HostGroup> hostGroups = createHostGroups(instanceGroups);

        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        ObjectNode rootNode = jsonNodeFactory.objectNode();
        ArrayNode hostGroupsNode = rootNode.putArray("host_groups");
        addHostGroup(hostGroupsNode, "gateway1", SL_MIN0_MAX3, KNOX);
        addHostGroup(hostGroupsNode, "gateway2", KNOX, MA_MIN1_MAX3);
        addHostGroup(hostGroupsNode, "master", SL_MIN0_MAX3, MA_MIN1_MAX5);
        BDDMockito.given(objectMapper.readTree(BLUEPRINT_STRING)).willReturn(rootNode);

        Cluster cluster = new Cluster();
        cluster.setSecure(true);
        Gateway gateway = new Gateway();
        gateway.setEnableGateway(true);
        cluster.setGateway(gateway);

        // WHEN
        underTest.validateBlueprintForStack(cluster, blueprint, hostGroups, instanceGroups);

        // THEN no exception thrown
    }

    private void setupStackServiceComponentDescriptors() {
        BDDMockito.given(stackServiceComponentDescriptors.get(MA_MIN1_MAX5)).willReturn(new StackServiceComponentDescriptor(MA_MIN1_MAX5, "MASTER", 1, 5));
        BDDMockito.given(stackServiceComponentDescriptors.get(MA_MIN1_MAX1)).willReturn(new StackServiceComponentDescriptor(MA_MIN1_MAX1, "MASTER", 1, 1));
        BDDMockito.given(stackServiceComponentDescriptors.get(MA_MIN1_MAX3)).willReturn(new StackServiceComponentDescriptor(MA_MIN1_MAX3, "MASTER", 1, 3));
        BDDMockito.given(stackServiceComponentDescriptors.get(SL_MIN0_MAX3)).willReturn(new StackServiceComponentDescriptor(SL_MIN0_MAX3, "SLAVE", 0, 3));
        BDDMockito.given(stackServiceComponentDescriptors.get(SL_MIN5_MAX6)).willReturn(new StackServiceComponentDescriptor(SL_MIN5_MAX6, "SLAVE", 5, 6));
        BDDMockito.given(stackServiceComponentDescriptors.get(KNOX)).willReturn(new StackServiceComponentDescriptor(KNOX, "MASTER", 0, 10));
    }

    private Blueprint createBlueprint() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(BLUEPRINT_STRING);
        return blueprint;
    }

    private Set<InstanceGroup> createInstanceGroups() {
        Set<InstanceGroup> groups = Sets.newHashSet();
        groups.add(createInstanceGroup(GROUP1, 0, 1));
        groups.add(createInstanceGroup(GROUP2, 1, 2));
        groups.add(createInstanceGroup(GROUP3, 3, 3));
        return groups;
    }

    private InstanceGroup createInstanceGroup(String groupName, long startingPrivateNumber, int nodeCount) {
        return createInstanceGroup(groupName, startingPrivateNumber, nodeCount, InstanceGroupType.CORE);
    }

    private InstanceGroup createInstanceGroup(String groupName, long startingPrivateNumber, int nodeCount, InstanceGroupType instanceGroupType) {
        InstanceGroup group = new InstanceGroup();
        group.setGroupName(groupName);
        Set<InstanceMetaData> instanceMetaDataSet = new HashSet<>();
        for (int i = 0; i < nodeCount; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setPrivateId(startingPrivateNumber);
            instanceMetaData.setInstanceStatus(com.sequenceiq.cloudbreak.api.model.InstanceStatus.REQUESTED);
            instanceMetaData.setInstanceGroup(group);
            instanceMetaDataSet.add(instanceMetaData);
            startingPrivateNumber++;
        }
        group.setInstanceMetaData(instanceMetaDataSet);
        group.setInstanceGroupType(instanceGroupType);
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
        Constraint constraint = new Constraint();
        constraint.setHostCount(instanceGroup.getNodeCount());
        constraint.setInstanceGroup(instanceGroup);
        group.setConstraint(constraint);
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
