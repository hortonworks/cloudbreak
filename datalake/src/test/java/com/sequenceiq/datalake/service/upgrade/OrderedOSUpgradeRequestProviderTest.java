package com.sequenceiq.datalake.service.upgrade;

import static com.sequenceiq.common.api.type.InstanceGroupName.ATLAS_SCALE_OUT;
import static com.sequenceiq.common.api.type.InstanceGroupName.AUXILIARY;
import static com.sequenceiq.common.api.type.InstanceGroupName.CORE;
import static com.sequenceiq.common.api.type.InstanceGroupName.GATEWAY;
import static com.sequenceiq.common.api.type.InstanceGroupName.HMS_SCALE_OUT;
import static com.sequenceiq.common.api.type.InstanceGroupName.IDBROKER;
import static com.sequenceiq.common.api.type.InstanceGroupName.KAFKA_SCALE_OUT;
import static com.sequenceiq.common.api.type.InstanceGroupName.MASTER;
import static com.sequenceiq.common.api.type.InstanceGroupName.RAZ_SCALE_OUT;
import static com.sequenceiq.common.api.type.InstanceGroupName.SOLR_SCALE_OUT;
import static com.sequenceiq.common.api.type.InstanceGroupName.STORAGE_SCALE_OUT;
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
        instanceGroups.add(createInstanceGroup(Set.of(createInstanceMetadata(SOLR_SCALE_OUT, 3))));
        instanceGroups.add(createInstanceGroup(Set.of(createInstanceMetadata(STORAGE_SCALE_OUT, 3))));
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(KAFKA_SCALE_OUT, 3),
                createInstanceMetadata(KAFKA_SCALE_OUT, 4)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(RAZ_SCALE_OUT, 3),
                createInstanceMetadata(RAZ_SCALE_OUT, 4),
                createInstanceMetadata(RAZ_SCALE_OUT, 5)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(ATLAS_SCALE_OUT, 3),
                createInstanceMetadata(ATLAS_SCALE_OUT, 4),
                createInstanceMetadata(ATLAS_SCALE_OUT, 5),
                createInstanceMetadata(ATLAS_SCALE_OUT, 6)
        )));
        instanceGroups.add(createInstanceGroup(Set.of(
                createInstanceMetadata(HMS_SCALE_OUT, 3),
                createInstanceMetadata(HMS_SCALE_OUT, 4)
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
                containsInAnyOrder(Set.of("i-solr_scale_out3", "i-storage_scale_out3", "i-kafka_scale_out3", "i-raz_scale_out3",
                        "i-atlas_scale_out3", "i-hms_scale_out3").toArray()));
        assertThat(actual.getOrderedOsUpgradeSets().get(4).getInstanceIds(),
                containsInAnyOrder(Set.of("i-kafka_scale_out4", "i-raz_scale_out4", "i-atlas_scale_out4", "i-hms_scale_out4").toArray()));
        assertThat(actual.getOrderedOsUpgradeSets().get(5).getInstanceIds(),
                containsInAnyOrder(Set.of("i-raz_scale_out5", "i-atlas_scale_out5").toArray()));
        assertThat(actual.getOrderedOsUpgradeSets().get(6).getInstanceIds(),
                containsInAnyOrder(Set.of("i-atlas_scale_out6").toArray()));
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