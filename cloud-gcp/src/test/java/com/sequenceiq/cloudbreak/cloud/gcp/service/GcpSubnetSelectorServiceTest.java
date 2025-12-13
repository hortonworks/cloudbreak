package com.sequenceiq.cloudbreak.cloud.gcp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;

public class GcpSubnetSelectorServiceTest {

    private GcpSubnetSelectorService underTest = new GcpSubnetSelectorService();

    @Test
    public void testSelectWhenNoSubnetMetadataShouldReturnError() {
        SubnetSelectionResult select = underTest.select(generateSubnetMetas(0), SubnetSelectionParameters.builder().build());
        assertEquals(select.getErrorMessage(), "GCP subnet selection: there are no subnets to choose from.");
    }

    @Test
    public void testSelectWhenNoSubnetSelectionParametersShouldReturnError() {
        SubnetSelectionResult select = underTest.select(generateSubnetMetas(1), null);
        assertEquals(select.getErrorMessage(), "GCP subnet selection: parameters were not specified.");
    }

    @Test
    public void testSelectWhenNoErrorShouldReturnFirstSubnet() {
        SubnetSelectionResult select = underTest.select(generateSubnetMetas(1), SubnetSelectionParameters.builder().build());
        assertEquals(1, select.getResult().size());
    }

    public Collection<CloudSubnet> generateSubnetMetas(int count) {
        List<CloudSubnet> subnetMetas = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            subnetMetas.add(
                    new CloudSubnet.Builder()
                    .id("" + i)
                    .name("name-" + i)
                    .build()
            );
        }
        return subnetMetas;
    }

}