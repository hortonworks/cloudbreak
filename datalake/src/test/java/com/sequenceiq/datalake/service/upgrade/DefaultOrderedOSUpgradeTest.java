package com.sequenceiq.datalake.service.upgrade;

import static com.sequenceiq.common.api.type.InstanceGroupName.AUXILIARY;
import static com.sequenceiq.common.api.type.InstanceGroupName.CORE;
import static com.sequenceiq.common.api.type.InstanceGroupName.GATEWAY;
import static com.sequenceiq.common.api.type.InstanceGroupName.IDBROKER;
import static com.sequenceiq.common.api.type.InstanceGroupName.MASTER;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSet;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.InstanceGroupName;

@ExtendWith(MockitoExtension.class)
class DefaultOrderedOSUpgradeTest {

    private static final String TARGET_IMAGE_ID = "target-image-id";

    @InjectMocks
    private DefaultOrderedOSUpgrade underTest;

    @Test
    void testCreateDatalakeOrderedOSUpgrade() {
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
                createPrimaryGatewayInstanceMetadata(GATEWAY, 0),
                createInstanceMetadata(GATEWAY, 1)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(createInstanceMetadata(AUXILIARY, 0))));

        List<OrderedOSUpgradeSet> actual = underTest.createDatalakeOrderedOSUpgrade(createStackV4Response(instanceGroups), TARGET_IMAGE_ID);

        assertEquals(0, actual.get(0).getOrder());
        assertEquals(1, actual.get(1).getOrder());
        assertEquals(2, actual.get(2).getOrder());
        assertEquals(3, actual.get(3).getOrder());
        assertThat(actual.get(0).getInstanceIds(),
                containsInAnyOrder(Arrays.asList("i-gateway0").toArray()));
        assertThat(actual.get(1).getInstanceIds(),
                containsInAnyOrder(Arrays.asList("i-master0", "i-core0", "i-gateway1", "i-idbroker0").toArray()));
        assertThat(actual.get(2).getInstanceIds(),
                containsInAnyOrder(Set.of("i-master1", "i-core1", "i-auxiliary0", "i-idbroker1").toArray()));
        assertThat(actual.get(3).getInstanceIds(),
                containsInAnyOrder(Set.of("i-core2").toArray()));
    }

    @Test
    void testCreateOrderedOSUpgradeShouldThrowExceptionWhenThereAreMissingInstancesFromTheRequest() {
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
                createPrimaryGatewayInstanceMetadata(GATEWAY, 0),
                createInstanceMetadata(GATEWAY, 1)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(createInstanceMetadata(AUXILIARY, 0))));

        Exception exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.createDatalakeOrderedOSUpgrade(createStackV4Response(instanceGroups), TARGET_IMAGE_ID));

        assertEquals("The following instances are missing from the ordered OS upgrade request: [i-gateway2]", exception.getMessage());
    }

    @Test
    void testCreateOrderedOSUpgradeShouldThrowExceptionWhenThereAreMissingInstances() {
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
                createPrimaryGatewayInstanceMetadata(GATEWAY, 0),
                createInstanceMetadata(GATEWAY, 1)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(createInstanceMetadata(AUXILIARY, 0))));

        assertThrows(CloudbreakServiceException.class,
                () -> underTest.createDatalakeOrderedOSUpgrade(createStackV4Response(instanceGroups), TARGET_IMAGE_ID));
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

    private InstanceMetaDataV4Response createPrimaryGatewayInstanceMetadata(InstanceGroupName instanceGroup, int order) {
        InstanceMetaDataV4Response instanceMetadata = createInstanceMetadata(instanceGroup, order);
        instanceMetadata.setInstanceType(InstanceMetadataType.GATEWAY_PRIMARY);
        return instanceMetadata;
    }

    private StackV4Response createStackV4Response(List<InstanceGroupV4Response> instanceGroups) {
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setInstanceGroups(instanceGroups);
        return stackV4Response;
    }
}
