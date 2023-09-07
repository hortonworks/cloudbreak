package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.azure.resourcemanager.network.models.Delegation;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;

class AzureCloudSubnetParametersServiceTest {
    private static final String FLEXIBLE_SERVER = "Microsoft.DBforPostgreSQL/flexibleServers";

    private AzureCloudSubnetParametersService underTest;

    @BeforeEach
    void setUp() {
        underTest = new AzureCloudSubnetParametersService();
    }

    @Test
    void testAddFlexibleServerDelegatedSubnetWithEmptyDelegation() {
        CloudSubnet cloudSubnet = new CloudSubnet();
        List<Delegation> delegations = new ArrayList<>();
        underTest.addFlexibleServerDelegatedSubnet(cloudSubnet, delegations);
        assertFalse(cloudSubnet.getParameter(AzureCloudSubnetParametersService.FLEXIBLE_SERVER_DELEGATED_SUBNET, Boolean.class));
    }

    @Test
    void testAddFlexibleServerDelegatedSubnetWithNullDelegation() {
        CloudSubnet cloudSubnet = new CloudSubnet();
        underTest.addFlexibleServerDelegatedSubnet(cloudSubnet, null);
        assertFalse(cloudSubnet.getParameter(AzureCloudSubnetParametersService.FLEXIBLE_SERVER_DELEGATED_SUBNET, Boolean.class));
    }

    @Test
    void testAddFlexibleServerDelegatedSubnetWithFlexibleDelegation() {
        CloudSubnet cloudSubnet = new CloudSubnet();
        List<Delegation> delegations = new ArrayList<>();
        delegations.add(new Delegation().withServiceName(FLEXIBLE_SERVER));
        delegations.add(new Delegation().withServiceName("testservice"));
        underTest.addFlexibleServerDelegatedSubnet(cloudSubnet, delegations);
        assertTrue(cloudSubnet.getParameter(AzureCloudSubnetParametersService.FLEXIBLE_SERVER_DELEGATED_SUBNET, Boolean.class));
    }

    @Test
    void testAddFlexibleServerDelegatedSubnetWithoutFlexibleDelegation() {
        CloudSubnet cloudSubnet = new CloudSubnet();
        List<Delegation> delegations = new ArrayList<>();
        delegations.add(new Delegation().withServiceName("testservice1"));
        delegations.add(new Delegation().withServiceName("testservice2"));
        underTest.addFlexibleServerDelegatedSubnet(cloudSubnet, delegations);
        assertFalse(cloudSubnet.getParameter(AzureCloudSubnetParametersService.FLEXIBLE_SERVER_DELEGATED_SUBNET, Boolean.class));
    }

    @Test
    void testIsFlexibleServerDelegatedSubnetWithNoParam() {
        CloudSubnet cloudSubnet = new CloudSubnet();
        boolean actualResult = underTest.isFlexibleServerDelegatedSubnet(cloudSubnet);
        assertFalse(actualResult);
    }

    @Test
    void testIsFlexibleServerDelegatedSubnetWithParam() {
        CloudSubnet cloudSubnet = new CloudSubnet();
        cloudSubnet.putParameter(AzureCloudSubnetParametersService.FLEXIBLE_SERVER_DELEGATED_SUBNET, Boolean.TRUE);
        boolean actualResult = underTest.isFlexibleServerDelegatedSubnet(cloudSubnet);
        assertTrue(actualResult);
    }
}
