package com.sequenceiq.environment.network.service;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.network.dto.NetworkDto;

class SubnetIdProviderTest {

    private static final String SUBNET_ID_1 = "subnetId1";

    private static final String SUBNET_ID_2 = "subnetId2";

    private static final String SUBNET_ID_3 = "subnetId3";

    private final SubnetIdProvider underTest = new SubnetIdProvider();

    @Test
    void testProvideShouldReturnAPrivateWhenCcmAws() {
        Map<String, CloudSubnet> subnetMetas = createSubnetMetas();
        NetworkDto networkDto = NetworkDto.builder()
                .withSubnetMetas(subnetMetas)
                .build();

        String actual = underTest.provide(networkDto, Tunnel.CCM, CloudPlatform.AWS);

        Assertions.assertTrue(StringUtils.isNotBlank(actual));
        Assertions.assertTrue(subnetMetas.get(actual).isPrivateSubnet());
    }

    @Test
    void testProvideShouldReturnAPublicSubnetIDWhenNoCcmAws() {
        Map<String, CloudSubnet> subnetMetas = createSubnetMetas();
        NetworkDto networkDto = NetworkDto.builder()
                .withSubnetMetas(subnetMetas)
                .build();

        String actual = underTest.provide(networkDto, Tunnel.DIRECT, CloudPlatform.AWS);

        Assertions.assertTrue(StringUtils.isNotBlank(actual));
        Assertions.assertFalse(subnetMetas.get(actual).isPrivateSubnet());
    }

    @Test
    void testProvideShouldReturnAPrivateSubnetIDWhenCcmNoPublicIpAws() {
        Map<String, CloudSubnet> subnetMetas = createSubnetMetasWithNoPublicIp();
        NetworkDto networkDto = NetworkDto.builder()
                .withSubnetMetas(subnetMetas)
                .build();

        String actual = underTest.provide(networkDto, Tunnel.CCM, CloudPlatform.AWS);

        Assertions.assertTrue(StringUtils.isNotBlank(actual));
        Assertions.assertTrue(subnetMetas.get(actual).isPrivateSubnet());
    }

    @Test
    void testProvideShouldReturnAPrivateSubnetIDWhenNoCcmNoPublicIpAws() {
        Map<String, CloudSubnet> subnetMetas = createSubnetMetasWithNoPublicIp();
        NetworkDto networkDto = NetworkDto.builder()
                .withSubnetMetas(subnetMetas)
                .build();

        String actual = underTest.provide(networkDto, Tunnel.DIRECT, CloudPlatform.AWS);

        Assertions.assertTrue(StringUtils.isNotBlank(actual));
        Assertions.assertTrue(subnetMetas.get(actual).isPrivateSubnet());
    }

    @Test
    void testProvideShouldReturnAPublicSubnetIDWhenCcmNoPrivateAws() {
        Map<String, CloudSubnet> subnetMetas = createPublicSubnetMetas();
        NetworkDto networkDto = NetworkDto.builder()
                .withSubnetMetas(subnetMetas)
                .build();

        String actual = underTest.provide(networkDto, Tunnel.CCM, CloudPlatform.AWS);

        Assertions.assertTrue(StringUtils.isNotBlank(actual));
        Assertions.assertFalse(subnetMetas.get(actual).isPrivateSubnet());
    }

    @Test
    void testProvideShouldReturnAPublicSubnetIDWhenNoCcmNoPrivateAws() {
        Map<String, CloudSubnet> subnetMetas = createPublicSubnetMetas();
        NetworkDto networkDto = NetworkDto.builder()
                .withSubnetMetas(subnetMetas)
                .build();

        String actual = underTest.provide(networkDto, Tunnel.DIRECT, CloudPlatform.AWS);

        Assertions.assertTrue(StringUtils.isNotBlank(actual));
        Assertions.assertFalse(subnetMetas.get(actual).isPrivateSubnet());
    }

    @Test
    void testProvideShouldReturnAPrivateWhenCcmAndNoPublicAws() {
        Map<String, CloudSubnet> subnetMetas = createPrivateSubnetMetas();
        NetworkDto networkDto = NetworkDto.builder()
                .withSubnetMetas(subnetMetas)
                .build();

        String actual = underTest.provide(networkDto, Tunnel.CCM, CloudPlatform.AWS);

        Assertions.assertTrue(StringUtils.isNotBlank(actual));
        Assertions.assertTrue(subnetMetas.get(actual).isPrivateSubnet());
    }

    @Test
    void testProvideShouldReturnAPrivateWhenNoCcmAndNoPublicAws() {
        Map<String, CloudSubnet> subnetMetas = createPrivateSubnetMetas();
        NetworkDto networkDto = NetworkDto.builder()
                .withSubnetMetas(subnetMetas)
                .build();

        String actual = underTest.provide(networkDto, Tunnel.DIRECT, CloudPlatform.AWS);

        Assertions.assertTrue(StringUtils.isNotBlank(actual));
        Assertions.assertTrue(subnetMetas.get(actual).isPrivateSubnet());
    }

    @Test
    void testProvideShouldReturnAPublicSubnetIDWhenCcmNoPrivateNoPublicIpAws() {
        Map<String, CloudSubnet> subnetMetas = createPublicSubnetMetasWithNoPublicIp();
        NetworkDto networkDto = NetworkDto.builder()
                .withSubnetMetas(subnetMetas)
                .build();

        String actual = underTest.provide(networkDto, Tunnel.CCM, CloudPlatform.AWS);

        Assertions.assertTrue(StringUtils.isNotBlank(actual));
        Assertions.assertFalse(subnetMetas.get(actual).isPrivateSubnet());
    }

    @Test
    void testProvideShouldReturnAPublicSubnetIDWhenNoCcmNoPrivateNoPublicIpAws() {
        Map<String, CloudSubnet> subnetMetas = createPublicSubnetMetasWithNoPublicIp();
        NetworkDto networkDto = NetworkDto.builder()
                .withSubnetMetas(subnetMetas)
                .build();

        String actual = underTest.provide(networkDto, Tunnel.DIRECT, CloudPlatform.AWS);

        Assertions.assertTrue(StringUtils.isNotBlank(actual));
        Assertions.assertFalse(subnetMetas.get(actual).isPrivateSubnet());
    }

    @Test
    void testProvideShouldReturnNullWhenNetworkNull() {
        String actual = underTest.provide(null, Tunnel.DIRECT, CloudPlatform.AWS);

        Assertions.assertNull(actual);
    }

    @Test
    void testProvideShouldReturnNullWhenNoSubnetMetas() {
        NetworkDto networkDto = NetworkDto.builder()
                .withSubnetMetas(Map.of())
                .build();

        String actual = underTest.provide(networkDto, Tunnel.DIRECT, CloudPlatform.AWS);

        Assertions.assertNull(actual);
    }

    @Test
    void testProvideShouldReturnASubnetAzure() {
        Map<String, CloudSubnet> subnetMetas = createSubnetMetas();
        NetworkDto networkDto = NetworkDto.builder()
                .withSubnetMetas(subnetMetas)
                .build();

        String actual = underTest.provide(networkDto, Tunnel.DIRECT, CloudPlatform.AZURE);

        Assertions.assertTrue(StringUtils.isNotBlank(actual));
    }

    private Map<String, CloudSubnet> createSubnetMetas() {
        return Map.of(
                SUBNET_ID_1, new CloudSubnet(SUBNET_ID_1, SUBNET_ID_1, "az", "cidr", true, true, false),
                SUBNET_ID_2, new CloudSubnet(SUBNET_ID_2, SUBNET_ID_2, "az", "cidr", false, true, true),
                SUBNET_ID_3, new CloudSubnet(SUBNET_ID_3, SUBNET_ID_3, "az", "cidr", true, true, false));
    }

    private Map<String, CloudSubnet> createSubnetMetasWithNoPublicIp() {
        return Map.of(
                SUBNET_ID_1, new CloudSubnet(SUBNET_ID_1, SUBNET_ID_1, "az", "cidr", true, false, false),
                SUBNET_ID_2, new CloudSubnet(SUBNET_ID_2, SUBNET_ID_2, "az", "cidr", false, false, true),
                SUBNET_ID_3, new CloudSubnet(SUBNET_ID_3, SUBNET_ID_3, "az", "cidr", true, false, false));
    }

    private Map<String, CloudSubnet> createPublicSubnetMetas() {
        return Map.of(
                SUBNET_ID_1, new CloudSubnet(SUBNET_ID_1, SUBNET_ID_1, "az", "cidr", false, true, true),
                SUBNET_ID_2, new CloudSubnet(SUBNET_ID_2, SUBNET_ID_2, "az", "cidr", false, true, true),
                SUBNET_ID_3, new CloudSubnet(SUBNET_ID_3, SUBNET_ID_3, "az", "cidr", false, true, true));
    }

    private Map<String, CloudSubnet> createPublicSubnetMetasWithNoPublicIp() {
        return Map.of(
                SUBNET_ID_1, new CloudSubnet(SUBNET_ID_1, SUBNET_ID_1, "az", "cidr", false, false, true),
                SUBNET_ID_2, new CloudSubnet(SUBNET_ID_2, SUBNET_ID_2, "az", "cidr", false, false, true),
                SUBNET_ID_3, new CloudSubnet(SUBNET_ID_3, SUBNET_ID_3, "az", "cidr", false, false, true));
    }

    private Map<String, CloudSubnet> createPrivateSubnetMetas() {
        return Map.of(
                SUBNET_ID_1, new CloudSubnet(SUBNET_ID_1, SUBNET_ID_1, "az", "cidr", true, true, false),
                SUBNET_ID_2, new CloudSubnet(SUBNET_ID_2, SUBNET_ID_2, "az", "cidr", true, true, false),
                SUBNET_ID_3, new CloudSubnet(SUBNET_ID_3, SUBNET_ID_3, "az", "cidr", true, true, false));
    }

}