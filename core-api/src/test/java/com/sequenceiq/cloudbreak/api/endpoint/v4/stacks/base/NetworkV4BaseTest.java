package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.GcpNetworkV4Parameters;

class NetworkV4BaseTest {

    NetworkV4Base underTest;

    @BeforeEach
    void setUp() {
        underTest = new NetworkV4Base();
    }

    @Test
    void testAzureNetworkPublicIpIsNotEmpty() {
        AzureNetworkV4Parameters networkParameters = new AzureNetworkV4Parameters();
        networkParameters.setNoPublicIp(true);
        underTest.setAzure(networkParameters);

        assertTrue(underTest.isNoPublicIp().get());
    }

    @Test
    void testGcpNetworkPublicIpIsNotEmpty() {
        GcpNetworkV4Parameters networkParameters = new GcpNetworkV4Parameters();
        networkParameters.setNoPublicIp(true);
        underTest.setGcp(networkParameters);

        assertTrue(underTest.isNoPublicIp().get());
    }

    @Test
    void testAwsNetworkPublicIpIsEmpty() {
        AwsNetworkV4Parameters networkParameters = new AwsNetworkV4Parameters();
        underTest.setAws(networkParameters);

        assertTrue(underTest.isNoPublicIp().isEmpty());
    }

    @Test
    void testIsNoPublicIpIsEmptyOnNull() {
        underTest.createAzure();

        assertTrue(underTest.isNoPublicIp().isEmpty());
    }

    @Test
    void testIsEmptyShouldReturnTrueForNewUntouchedInstance() {
        assertTrue(underTest.isEmpty());
    }

    @Test
    void testIsEmptyShouldReturnFalseForEditedInstance() {
        underTest.setAws(new AwsNetworkV4Parameters());
        assertFalse(underTest.isEmpty());
    }

}