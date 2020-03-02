package com.sequenceiq.cloudbreak.cloud.azure.subnet.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;

public class AzureSubnetSelectorServiceTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private AzureSubnetSelectorService underTest = new AzureSubnetSelectorService();

    @Test
    public void testSelectWhenNoSubnetsThenThrows() {
        List<CloudSubnet> subnets = new SubnetBuilder().build();
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Azure subnet selection: there are no subnets to choose from.");

        underTest.select(subnets, SubnetSelectionParameters.builder().build());
    }

    @Test
    public void testSelectWhenSubnetmetasNullThenThrows() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Azure subnet selection: there are no subnets to choose from.");

        underTest.select(null, SubnetSelectionParameters.builder().build());
    }

    @Test
    public void testSelectWhenParametersNullThenThrows() {
        List<CloudSubnet> subnets = new SubnetBuilder().withPrivateSubnet().build();
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Azure subnet selection: parameters were not specified.");

        underTest.select(subnets, null);
    }

    @Test
    public void testselectWhenNotDatabaseOneSubnetThenReturnsIt() {
        List<CloudSubnet> subnets = new SubnetBuilder().withPrivateSubnet().build();

        List<CloudSubnet> selectedSubnets = underTest.select(subnets, SubnetSelectionParameters.builder().build());

        assertEquals(1, selectedSubnets.size());
    }

    @Test
    public void testselectWhenNotDatabaseTwoSubnetsThenOneIsSelected() {
        List<CloudSubnet> subnets = new SubnetBuilder().withPrivateSubnet().withPrivateSubnet().build();

        List<CloudSubnet> selectedSubnets = underTest.select(subnets, SubnetSelectionParameters.builder().build());

        assertEquals(1, selectedSubnets.size());
    }

    @Test
    public void testselectWhenDatabaseThreeSubnetsThenAllAreReturned() {
        List<CloudSubnet> subnets = new SubnetBuilder().withPrivateSubnet().withPrivateSubnet().withPrivateSubnet().build();

        List<CloudSubnet> selectedSubnets = underTest.select(subnets, SubnetSelectionParameters.builder().withForDatabase().build());

        assertEquals(3, selectedSubnets.size());
        List<String> selectedSubnetIds = selectedSubnets.stream().map(CloudSubnet::getId).collect(Collectors.toList());
        assertThat(selectedSubnetIds, hasItem("subnet-1"));
        assertThat(selectedSubnetIds, hasItem("subnet-2"));
        assertThat(selectedSubnetIds, hasItem("subnet-3"));
    }

}
