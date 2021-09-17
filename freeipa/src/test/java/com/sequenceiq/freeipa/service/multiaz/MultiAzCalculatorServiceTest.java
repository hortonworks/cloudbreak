package com.sequenceiq.freeipa.service.multiaz;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_IDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;
import com.sequenceiq.freeipa.entity.InstanceMetaData;

@ExtendWith(MockitoExtension.class)
class MultiAzCalculatorServiceTest {

    private static final String SUB_1 = "SUB1";

    private static final String SUB_2 = "SUB2";

    private static final Map<String, String> SUBNET_AZ_PAIRS = Map.of(SUB_1, "AZ1", SUB_2, "AZ2", "ONLYINAZMAP", "AZ3");

    @Mock
    private MultiAzValidator multiAzValidator;

    @InjectMocks
    private MultiAzCalculatorService underTest;

    @Test
    public void testCalculateCurrentSubnetUsage() {
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        instanceGroupNetwork.setAttributes(Json.silent(Map.of(SUBNET_IDS, List.of(SUB_1, SUB_2, "ONLYINIG"))));
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupNetwork(instanceGroupNetwork);
        InstanceMetaData deletedInstance = createInstanceMetadata(SUB_1);
        deletedInstance.setInstanceStatus(InstanceStatus.TERMINATED);
        instanceGroup.setInstanceMetaData(Set.of(createInstanceMetadata(SUB_1), createInstanceMetadata(SUB_1), createInstanceMetadata(SUB_2),
                createInstanceMetadata(null), createInstanceMetadata(" "), createInstanceMetadata("IGNORED"), deletedInstance));

        Map<String, Integer> result = underTest.calculateCurrentSubnetUsage(SUBNET_AZ_PAIRS, instanceGroup);

        assertEquals(2, result.size());
        assertEquals(2, result.get(SUB_1));
        assertEquals(1, result.get(SUB_2));
    }

    @Test
    public void testUpdateSubnetIdForSingleInstanceIfEligible() {
        InstanceGroup instanceGroup = new InstanceGroup();
        when(multiAzValidator.supportedForInstanceMetadataGeneration(instanceGroup)).thenReturn(Boolean.TRUE);
        InstanceMetaData instanceMetaData = new InstanceMetaData();

        underTest.updateSubnetIdForSingleInstanceIfEligible(SUBNET_AZ_PAIRS, new HashMap<>(Map.of(SUB_1, 2, SUB_2, 1)), instanceMetaData, instanceGroup);

        assertEquals(SUB_2, instanceMetaData.getSubnetId());
    }

    @Test
    public void testUpdateSubnetIdForSingleInstanceIfEligibleValidatorReturnFalse() {
        InstanceGroup instanceGroup = new InstanceGroup();
        when(multiAzValidator.supportedForInstanceMetadataGeneration(instanceGroup)).thenReturn(Boolean.FALSE);
        InstanceMetaData instanceMetaData = new InstanceMetaData();

        underTest.updateSubnetIdForSingleInstanceIfEligible(SUBNET_AZ_PAIRS, new HashMap<>(Map.of(SUB_1, 2, SUB_2, 1)), instanceMetaData, instanceGroup);

        assertNull(instanceMetaData.getSubnetId());
    }

    @Test
    public void testUpdateSubnetIdForSingleInstanceIfEligibleSubnetUsageEmpty() {
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData instanceMetaData = new InstanceMetaData();

        underTest.updateSubnetIdForSingleInstanceIfEligible(SUBNET_AZ_PAIRS, new HashMap<>(), instanceMetaData, instanceGroup);

        assertNull(instanceMetaData.getSubnetId());
    }

    @Test
    public void testUpdateSubnetIdForSingleInstanceIfEligibleDontModifyExisting() {
        InstanceGroup instanceGroup = new InstanceGroup();
        when(multiAzValidator.supportedForInstanceMetadataGeneration(instanceGroup)).thenReturn(Boolean.TRUE);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setSubnetId("ASDF");

        underTest.updateSubnetIdForSingleInstanceIfEligible(SUBNET_AZ_PAIRS, new HashMap<>(Map.of(SUB_1, 2, SUB_2, 1)), instanceMetaData, instanceGroup);

        assertEquals("ASDF", instanceMetaData.getSubnetId());
    }

    @Test
    public void testCalculateRoundRobin() {
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        instanceGroupNetwork.setAttributes(Json.silent(Map.of(SUBNET_IDS, List.of(SUB_1, SUB_2, "ONLYINIG"))));
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupNetwork(instanceGroupNetwork);
        InstanceMetaData deletedInstance = createInstanceMetadata(SUB_1);
        deletedInstance.setInstanceStatus(InstanceStatus.TERMINATED);
        instanceGroup.setInstanceMetaData(Set.of(createInstanceMetadata(null), createInstanceMetadata(null), createInstanceMetadata(null),
                createInstanceMetadata(SUB_2), createInstanceMetadata(null), createInstanceMetadata(" "), createInstanceMetadata("IGNORED"),
                deletedInstance));
        when(multiAzValidator.supportedForInstanceMetadataGeneration(instanceGroup)).thenReturn(Boolean.TRUE);

        underTest.calculateByRoundRobin(SUBNET_AZ_PAIRS, instanceGroup);

        assertEquals(3, instanceGroup.getNotDeletedInstanceMetaDataSet().stream()
                .filter(im -> SUB_1.equals(im.getSubnetId()) && "AZ1".equals(im.getAvailabilityZone())).count());
        assertEquals(2, instanceGroup.getNotDeletedInstanceMetaDataSet().stream()
                .filter(im -> SUB_2.equals(im.getSubnetId()) && "AZ2".equals(im.getAvailabilityZone())).count());
        assertEquals(3, instanceGroup.getNotDeletedInstanceMetaDataSet().stream()
                .filter(im -> SUB_2.equals(im.getSubnetId())).count());
        assertEquals(1, instanceGroup.getNotDeletedInstanceMetaDataSet().stream().filter(im -> "IGNORED".equals(im.getSubnetId())).count());
    }

    private InstanceMetaData createInstanceMetadata(String subnetId) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setSubnetId(subnetId);
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        return instanceMetaData;
    }
}