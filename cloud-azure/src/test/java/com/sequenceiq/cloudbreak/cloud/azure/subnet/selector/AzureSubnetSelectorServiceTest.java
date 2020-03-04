package com.sequenceiq.cloudbreak.cloud.azure.subnet.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;

public class AzureSubnetSelectorServiceTest {

    private AzureSubnetSelectorService underTest = new AzureSubnetSelectorService();

    @Test
    public void testSelectWhenNoSubnetsThenReturnsError() {
        List<CloudSubnet> subnets = new SubnetBuilder().build();

        SubnetSelectionResult subnetSelectionResult = underTest.select(subnets, SubnetSelectionParameters.builder().build());

        assertTrue(subnetSelectionResult.hasError());
        assertEquals("Azure subnet selection: there are no subnets to choose from.", subnetSelectionResult.getErrorMessage());
    }

    @Test
    public void testSelectWhenSubnetmetasNullThenReturnsError() {

        SubnetSelectionResult subnetSelectionResult = underTest.select(null, SubnetSelectionParameters.builder().build());

        assertTrue(subnetSelectionResult.hasError());
        assertEquals("Azure subnet selection: there are no subnets to choose from.", subnetSelectionResult.getErrorMessage());
    }

    @Test
    public void testSelectWhenParametersNullThenReturnsError() {
        List<CloudSubnet> subnets = new SubnetBuilder().withPrivateSubnet().build();

        SubnetSelectionResult subnetSelectionResult = underTest.select(subnets, null);

        assertTrue(subnetSelectionResult.hasError());
        assertEquals("Azure subnet selection: parameters were not specified.", subnetSelectionResult.getErrorMessage());
    }

    @Test
    public void testselectWhenNotDatabaseOneSubnetThenReturnsIt() {
        List<CloudSubnet> subnets = new SubnetBuilder().withPrivateSubnet().build();

        SubnetSelectionResult subnetSelectionResult = underTest.select(subnets, SubnetSelectionParameters.builder().build());

        assertEquals(1, subnetSelectionResult.getResult().size());
    }

    @Test
    public void testselectWhenNotDatabaseTwoSubnetsThenOneIsSelected() {
        List<CloudSubnet> subnets = new SubnetBuilder().withPrivateSubnet().withPrivateSubnet().build();

        SubnetSelectionResult subnetSelectionResult = underTest.select(subnets, SubnetSelectionParameters.builder().build());

        assertEquals(1, subnetSelectionResult.getResult().size());
    }

    @Test
    public void testselectWhenDatabaseThreeSubnetsThenAllAreReturned() {
        List<CloudSubnet> subnets = new SubnetBuilder().withPrivateSubnet().withPrivateSubnet().withPrivateSubnet().build();

        SubnetSelectionResult subnetSelectionResult = underTest.select(subnets, SubnetSelectionParameters.builder().withForDatabase().build());

        assertEquals(3, subnetSelectionResult.getResult().size());
        List<String> selectedSubnetIds = subnetSelectionResult.getResult().stream().map(CloudSubnet::getId).collect(Collectors.toList());
        assertThat(selectedSubnetIds, hasItem("subnet-1"));
        assertThat(selectedSubnetIds, hasItem("subnet-2"));
        assertThat(selectedSubnetIds, hasItem("subnet-3"));
    }
}
