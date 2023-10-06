package com.sequenceiq.cloudbreak.cloud.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.azure.resourcemanager.network.fluent.models.SubnetInner;
import com.azure.resourcemanager.network.models.Subnet;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

class AzureAddressPrefixProviderTest {

    private AzureAddressPrefixProvider underTest = new AzureAddressPrefixProvider();

    @Test
    void whenAddressPefixPresentedOnRootLevelShouldReturnWithThat() {
        Subnet subnet = mock(Subnet.class);

        when(subnet.addressPrefix()).thenReturn("10.0.0.0/16");

        String addressPrefix = underTest.getAddressPrefix(subnet);
        assertTrue("10.0.0.0/16".equals(addressPrefix));
    }

    @Test
    void whenAddressPefixNullPresentedOnRootLevelShouldReturnWithInnerSubnet() {
        Subnet subnet = mock(Subnet.class);
        SubnetInner subnetInner = mock(SubnetInner.class);

        when(subnet.addressPrefix()).thenReturn(null);
        when(subnetInner.addressPrefixes()).thenReturn(List.of("10.0.0.0/16"));
        when(subnet.innerModel()).thenReturn(subnetInner);

        String addressPrefix = underTest.getAddressPrefix(subnet);
        assertTrue("10.0.0.0/16".equals(addressPrefix));
    }

    @Test
    void whenAddressPefixNullPresentedOnRootLevelAndInnerSubnetIsNullShouldReturnWithNull() {
        Subnet subnet = mock(Subnet.class);
        SubnetInner subnetInner = mock(SubnetInner.class);

        when(subnet.addressPrefix()).thenReturn(null);
        when(subnet.id()).thenReturn("id");
        when(subnetInner.addressPrefixes()).thenReturn(List.of());
        when(subnet.innerModel()).thenReturn(subnetInner);

        verifyException(() -> underTest.getAddressPrefix(subnet), "Your subnet id does not have a proper address prefix. Please fix that.");
    }

    private void verifyException(Executable executable, String messageExpected) {
        BadRequestException badRequestException = assertThrows(BadRequestException.class, executable);
        assertThat(badRequestException.getMessage()).isEqualTo(messageExpected);
        assertThat(badRequestException.getClass()).isEqualTo(BadRequestException.class);
    }

}