package com.sequenceiq.datalake.service.upgrade;

import static com.sequenceiq.common.api.type.InstanceGroupName.ATLASHG;
import static com.sequenceiq.common.api.type.InstanceGroupName.AUXILIARY;
import static com.sequenceiq.common.api.type.InstanceGroupName.CORE;
import static com.sequenceiq.common.api.type.InstanceGroupName.GATEWAY;
import static com.sequenceiq.common.api.type.InstanceGroupName.HMSHG;
import static com.sequenceiq.common.api.type.InstanceGroupName.IDBROKER;
import static com.sequenceiq.common.api.type.InstanceGroupName.KAFKAHG;
import static com.sequenceiq.common.api.type.InstanceGroupName.MASTER;
import static com.sequenceiq.common.api.type.InstanceGroupName.RAZHG;
import static com.sequenceiq.common.api.type.InstanceGroupName.SOLRHG;
import static com.sequenceiq.common.api.type.InstanceGroupName.STORAGEHG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSetRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.InstanceGroupName;

@ExtendWith(MockitoExtension.class)
class OrderedOSUpgradeRequestProviderTest {

    private static final String TARGET_IMAGE_ID = "target-image-id";

    @InjectMocks
    private OrderedOSUpgradeRequestProvider underTest;

    @Test
    void testCreateOrderedOSUpgradeSetRequestForMediumDutyDL() {
        List<InstanceGroupV4Response> instanceGroups = new ArrayList<>();
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(CORE, 0),
                createInstanceMetadata(CORE, 1),
                createInstanceMetadata(CORE, 2)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(MASTER, 0),
                createInstanceMetadata(MASTER, 1)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(IDBROKER, 0),
                createInstanceMetadata(IDBROKER, 1)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(GATEWAY, 0),
                createInstanceMetadata(GATEWAY, 1)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(createInstanceMetadata(AUXILIARY, 0))));
        OrderedOSUpgradeSetRequest actual = underTest.createDatalakeOrderedOSUpgradeSetRequest(createStackV4Response(instanceGroups), TARGET_IMAGE_ID);

        assertEquals(TARGET_IMAGE_ID, actual.getImageId());
        assertEquals(0, actual.getOrderedOsUpgradeSets().get(0).getOrder());
        assertEquals(1, actual.getOrderedOsUpgradeSets().get(1).getOrder());
        assertEquals(2, actual.getOrderedOsUpgradeSets().get(2).getOrder());
        assertThat(actual.getOrderedOsUpgradeSets().get(0).getInstanceIds(),
                containsInAnyOrder(Arrays.asList("i-master0", "i-core0", "i-auxiliary0", "i-idbroker0").toArray()));
        assertThat(actual.getOrderedOsUpgradeSets().get(1).getInstanceIds(),
                containsInAnyOrder(Set.of("i-master1", "i-core1", "i-gateway0", "i-idbroker1").toArray()));
        assertThat(actual.getOrderedOsUpgradeSets().get(2).getInstanceIds(),
                containsInAnyOrder(Set.of("i-core2", "i-gateway1").toArray()));
    }

    @Test
    void testCreateOrderedOSUpgradeSetRequestForEnterpriseDl() {
        List<InstanceGroupV4Response> instanceGroups = new ArrayList<>();
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(CORE, 0),
                createInstanceMetadata(CORE, 1),
                createInstanceMetadata(CORE, 2)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(MASTER, 0),
                createInstanceMetadata(MASTER, 1)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(IDBROKER, 0),
                createInstanceMetadata(IDBROKER, 1)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(GATEWAY, 0),
                createInstanceMetadata(GATEWAY, 1)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(createInstanceMetadata(AUXILIARY, 0))));
        instanceGroups.add(createInstanceGroup(Set.of(createInstanceMetadata(SOLRHG, 3))));
        instanceGroups.add(createInstanceGroup(Set.of(createInstanceMetadata(STORAGEHG, 3))));
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(KAFKAHG, 3),
                createInstanceMetadata(KAFKAHG, 4)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(RAZHG, 3),
                createInstanceMetadata(RAZHG, 4),
                createInstanceMetadata(RAZHG, 5)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(ATLASHG, 3),
                createInstanceMetadata(ATLASHG, 4),
                createInstanceMetadata(ATLASHG, 5),
                createInstanceMetadata(ATLASHG, 6)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(HMSHG, 3),
                createInstanceMetadata(HMSHG, 4)
        )));

        OrderedOSUpgradeSetRequest actual = underTest.createDatalakeOrderedOSUpgradeSetRequest(createStackV4Response(instanceGroups), TARGET_IMAGE_ID);

        assertEquals(TARGET_IMAGE_ID, actual.getImageId());
        assertEquals(0, actual.getOrderedOsUpgradeSets().get(0).getOrder());
        assertEquals(1, actual.getOrderedOsUpgradeSets().get(1).getOrder());
        assertEquals(2, actual.getOrderedOsUpgradeSets().get(2).getOrder());
        assertEquals(3, actual.getOrderedOsUpgradeSets().get(3).getOrder());
        assertEquals(4, actual.getOrderedOsUpgradeSets().get(4).getOrder());
        assertEquals(5, actual.getOrderedOsUpgradeSets().get(5).getOrder());
        assertEquals(6, actual.getOrderedOsUpgradeSets().get(6).getOrder());

        assertThat(actual.getOrderedOsUpgradeSets().get(0).getInstanceIds(),
                containsInAnyOrder(Arrays.asList("i-master0", "i-core0", "i-auxiliary0", "i-idbroker0").toArray()));
        assertThat(actual.getOrderedOsUpgradeSets().get(1).getInstanceIds(),
                containsInAnyOrder(Set.of("i-master1", "i-core1", "i-gateway0", "i-idbroker1").toArray()));
        assertThat(actual.getOrderedOsUpgradeSets().get(2).getInstanceIds(),
                containsInAnyOrder(Set.of("i-core2", "i-gateway1").toArray()));
        assertThat(actual.getOrderedOsUpgradeSets().get(3).getInstanceIds(),
                containsInAnyOrder(Set.of("i-solrhg3", "i-storagehg3", "i-kafkahg3", "i-razhg3", "i-atlashg3", "i-hmshg3").toArray()));
        assertThat(actual.getOrderedOsUpgradeSets().get(4).getInstanceIds(),
                containsInAnyOrder(Set.of("i-kafkahg4", "i-razhg4", "i-atlashg4", "i-hmshg4").toArray()));
        assertThat(actual.getOrderedOsUpgradeSets().get(5).getInstanceIds(),
                containsInAnyOrder(Set.of("i-razhg5", "i-atlashg5").toArray()));
        assertThat(actual.getOrderedOsUpgradeSets().get(6).getInstanceIds(),
                containsInAnyOrder(Set.of("i-atlashg6").toArray()));
    }

    @Test
    void testCreateOrderedOSUpgradeSetRequestShouldThrowExceptionWhenThereAreMissingInstancesFromTheRequest() {
        List<InstanceGroupV4Response> instanceGroups = new ArrayList<>();
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(CORE, 0),
                createInstanceMetadata(CORE, 1),
                createInstanceMetadata(CORE, 2)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(MASTER, 0),
                createInstanceMetadata(MASTER, 1)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(IDBROKER, 0),
                createInstanceMetadata(IDBROKER, 1)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(GATEWAY, 2),
                createInstanceMetadata(GATEWAY, 0),
                createInstanceMetadata(GATEWAY, 1)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(createInstanceMetadata(AUXILIARY, 0))));

        Exception exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.createDatalakeOrderedOSUpgradeSetRequest(createStackV4Response(instanceGroups), TARGET_IMAGE_ID));

        assertEquals("The following instances are missing from the ordered OS upgrade request: [i-gateway2]", exception.getMessage());
    }

    @Test
    void testCreateOrderedOSUpgradeSetRequestShouldThrowExceptionWhenThereAreMissingInstances() {
        List<InstanceGroupV4Response> instanceGroups = new ArrayList<>();
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(CORE, 0),
                createInstanceMetadata(CORE, 2)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(MASTER, 0),
                createInstanceMetadata(MASTER, 1)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(IDBROKER, 0),
                createInstanceMetadata(IDBROKER, 1)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(GATEWAY, 0),
                createInstanceMetadata(GATEWAY, 1)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(createInstanceMetadata(AUXILIARY, 0))));

        assertThrows(CloudbreakServiceException.class,
                () -> underTest.createDatalakeOrderedOSUpgradeSetRequest(createStackV4Response(instanceGroups), TARGET_IMAGE_ID));
    }

    private InstanceGroupV4Response createInstanceGroup(Set<InstanceMetaDataV4Response> instanceMetaDataV4Responses) {
        InstanceGroupV4Response instanceGroupV4Response = new InstanceGroupV4Response();
        instanceGroupV4Response.setName(instanceMetaDataV4Responses.iterator().next().getInstanceGroup());
        instanceGroupV4Response.setMetadata(instanceMetaDataV4Responses);
        return instanceGroupV4Response;
    }

    private InstanceMetaDataV4Response createInstanceMetadata(InstanceGroupName instanceGroup, int order) {
        InstanceMetaDataV4Response instanceMetaDataV4Response = new InstanceMetaDataV4Response();
        instanceMetaDataV4Response.setInstanceGroup(instanceGroup.getName());
        instanceMetaDataV4Response.setInstanceId("i-" + instanceGroup.getName() + order);
        instanceMetaDataV4Response.setDiscoveryFQDN("test-dl-" + instanceGroup.getName() + order + ".test-env.cloudera.site");
        return instanceMetaDataV4Response;
    }

    private StackV4Response createStackV4Response(List<InstanceGroupV4Response> instanceGroups) {
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setInstanceGroups(instanceGroups);
        return stackV4Response;
    }
}