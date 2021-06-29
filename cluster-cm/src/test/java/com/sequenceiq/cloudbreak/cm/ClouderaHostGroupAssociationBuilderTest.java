package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cluster.model.ClusterHostAttributes.FQDN;
import static com.sequenceiq.cloudbreak.cluster.model.ClusterHostAttributes.RACK_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

public class ClouderaHostGroupAssociationBuilderTest {

    private static final String HOSTGROUP_NAME_1 = "hostgroup-1";

    private static final String HOSTGROUP_NAME_2 = "hostgroup-2";

    private static final String FQDN_1 = "fqdn-1";

    private static final String FQDN_2 = "fqdn-2";

    private static final String FQDN_3 = "fqdn-3";

    private static final String FQDN_4 = "fqdn-4";

    private static final String RACK_ID_1 = "/dc-1/rack-2";

    private static final String RACK_ID_2 = "";

    private ClouderaHostGroupAssociationBuilder underTest;

    @BeforeEach
    void setUp() {
        underTest = new ClouderaHostGroupAssociationBuilder();
    }

    @Test
    public void testBuildHostGroupAssociationsShouldReturnsHostGroupAssociationsWhenTheParametersArePresent() {
        Map<HostGroup, List<InstanceMetaData>> instanceMetaDataByHostGroup = createInstanceMetaDataByHostGroup();

        Map<String, List<Map<String, String>>> actual = underTest.buildHostGroupAssociations(instanceMetaDataByHostGroup);

        assertEquals(FQDN_1, actual.get(HOSTGROUP_NAME_1).get(0).get(FQDN));
        assertEquals(FQDN_2, actual.get(HOSTGROUP_NAME_1).get(1).get(FQDN));
        assertEquals(FQDN_3, actual.get(HOSTGROUP_NAME_2).get(0).get(FQDN));
        assertEquals(FQDN_4, actual.get(HOSTGROUP_NAME_2).get(1).get(FQDN));
        assertThat(actual.get(HOSTGROUP_NAME_1).get(0).get(RACK_ID)).isEqualTo(RACK_ID_1);
        assertThat(actual.get(HOSTGROUP_NAME_1).get(1).get(RACK_ID)).isEqualTo(RACK_ID_1);
        assertThat(actual.get(HOSTGROUP_NAME_2).get(0).get(RACK_ID)).isEqualTo(RACK_ID_2);
        assertThat(actual.get(HOSTGROUP_NAME_2).get(1)).doesNotContainKey(RACK_ID);
    }

    @Test
    public void testBuildHostGroupAssociationsShouldReturnsEmptyMapWhenTheParameterEmpty() {
        Map<String, List<Map<String, String>>> actual = underTest.buildHostGroupAssociations(Collections.emptyMap());

        assertEquals(Collections.emptyMap(), actual);
    }

    private Map<HostGroup, List<InstanceMetaData>> createInstanceMetaDataByHostGroup() {
        Map<HostGroup, List<InstanceMetaData>> map = new HashMap<>();
        map.put(createHostGroup(HOSTGROUP_NAME_1), List.of(createInstanceMetaData(FQDN_1, RACK_ID_1), createInstanceMetaData(FQDN_2, RACK_ID_1)));
        map.put(createHostGroup(HOSTGROUP_NAME_2), List.of(createInstanceMetaData(FQDN_3, RACK_ID_2), createInstanceMetaData(FQDN_4, null)));
        return map;
    }

    private HostGroup createHostGroup(String name) {
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName(name);
        return hostGroup;
    }

    private InstanceMetaData createInstanceMetaData(String discoveryFQDN, String rackId) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN(discoveryFQDN);
        instanceMetaData.setRackId(rackId);
        return instanceMetaData;
    }
}