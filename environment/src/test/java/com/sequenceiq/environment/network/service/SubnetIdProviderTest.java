package com.sequenceiq.environment.network.service;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;

class SubnetIdProviderTest {

    private static final String SUBNET_ID_1 = "subnetId1";

    private static final String SUBNET_ID_2 = "subnetId2";

    private static final String SUBNET_ID_3 = "subnetId3";

    private SubnetIdProvider underTest = new SubnetIdProvider();

    @Test
    void testProvideShouldReturnARandomIDWhenTheNetworkIsExisting() {
        Map<String, CloudSubnet> subnetMetas = createSubnetMetas();
        NetworkDto networkDto = NetworkDto.builder()
                .withRegistrationType(RegistrationType.EXISTING)
                .withSubnetMetas(subnetMetas)
                .build();

        String actual = underTest.provide(networkDto);

        Assertions.assertTrue(subnetMetas.containsKey(actual));
    }

    @Test
    void testProvideShouldReturnAPublicSubnetIDWhenTheNetworkIsNew() {
        Map<String, CloudSubnet> subnetMetas = createSubnetMetas();
        NetworkDto networkDto = NetworkDto.builder()
                .withRegistrationType(RegistrationType.CREATE_NEW)
                .withSubnetMetas(subnetMetas)
                .build();

        String actual = underTest.provide(networkDto);

        Assertions.assertFalse(subnetMetas.get(actual).isPrivateSubnet());
    }

    private Map<String, CloudSubnet> createSubnetMetas() {
        return Map.of(
                SUBNET_ID_1, new CloudSubnet(SUBNET_ID_1, SUBNET_ID_1, "az", "cidr", true, true, true),
                SUBNET_ID_2, new CloudSubnet(SUBNET_ID_2, SUBNET_ID_2, "az", "cidr", false, true, true),
                SUBNET_ID_3, new CloudSubnet(SUBNET_ID_3, SUBNET_ID_3, "az", "cidr", true, true, true));
    }

}