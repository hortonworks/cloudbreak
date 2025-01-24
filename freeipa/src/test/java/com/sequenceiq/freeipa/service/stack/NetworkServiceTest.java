package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.filter.NetworkFilterProvider;

@ExtendWith(MockitoExtension.class)
class NetworkServiceTest {

    public static final String ENV_CRN = "ENV_CRN";

    public static final String NETWORK_ID = "networkId";

    public static final String SUBNET_1 = "subnet1";

    public static final String SUBNET_2 = "subnet2";

    public static final String REGION = "region";

    public static final String PLATFORM = "AWS";

    @Mock
    private CloudParameterService cloudParameterService;

    @Mock
    private CredentialService credentialService;

    @Mock
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Mock
    private Map<CloudPlatform, NetworkFilterProvider> networkFilterProviderMap;

    @InjectMocks
    private NetworkService underTest;

    @Test
    public void testFiltering() {
        Stack stack = new Stack();
        stack.setCloudPlatform(PLATFORM);
        stack.setRegion(REGION);
        stack.setPlatformvariant(PLATFORM);
        Credential credential = new Credential(PLATFORM, "", "", "", "acc");
        ExtendedCloudCredential extendedCred = new ExtendedCloudCredential(new CloudCredential(), PLATFORM, "", "", new ArrayList<>());
        CloudSubnet subnet1 = new CloudSubnet.Builder()
                .id(SUBNET_1)
                .name(SUBNET_1)
                .availabilityZone("")
                .cidr("10.1.0.0/24")
                .build();
        CloudSubnet subnet2 = new CloudSubnet.Builder()
                .id(SUBNET_2)
                .name(SUBNET_2)
                .availabilityZone("")
                .cidr("10.1.1.0/24")
                .build();
        CloudSubnet subnet3 = new CloudSubnet.Builder()
                .id("indifferent")
                .name("indifferent")
                .availabilityZone("")
                .cidr("10.1.2.0/24")
                .build();
        CloudNetwork cloudNetwork1 = new CloudNetwork(NETWORK_ID, NETWORK_ID, Set.of(subnet1, subnet2, subnet3), Map.of());
        CloudNetwork cloudNetwork2 = new CloudNetwork("other", "other",
                Set.of(new CloudSubnet.Builder()
                            .id(SUBNET_1)
                            .name(SUBNET_1)
                            .build(),
                        new CloudSubnet.Builder()
                            .id("test")
                            .name("test")
                            .build()
                ),
                Map.of());
        Map<String, Set<CloudNetwork>> cloudNets = Map.of(REGION, Set.of(cloudNetwork1, cloudNetwork2));
        CloudNetworks cloudNetworks = new CloudNetworks(cloudNets);

        when(credentialService.getCredentialByEnvCrn(ENV_CRN)).thenReturn(credential);
        when(extendedCloudCredentialConverter.convert(credential)).thenReturn(extendedCred);
        when(cloudParameterService.getCloudNetworks(eq(extendedCred), eq(REGION), eq(PLATFORM), any())).thenReturn(cloudNetworks);
        when(networkFilterProviderMap.get(any())).thenReturn(null);

        Multimap<String, String> filteredSubnetWithCidr = underTest.getFilteredSubnetWithCidr(ENV_CRN, stack, NETWORK_ID, List.of(SUBNET_1, SUBNET_2));

        assertEquals(2, filteredSubnetWithCidr.size());
        assertEquals(1, filteredSubnetWithCidr.get(subnet1.getId()).size());
        assertEquals(subnet1.getCidr(), filteredSubnetWithCidr.get(subnet1.getId()).stream().findFirst().get());
        assertEquals(1, filteredSubnetWithCidr.get(subnet2.getId()).size());
        assertEquals(subnet2.getCidr(), filteredSubnetWithCidr.get(subnet2.getId()).stream().findFirst().get());
    }

    @Test
    public void testAzureMultipleNetworkWithSameId() {
        Stack stack = new Stack();
        stack.setCloudPlatform(PLATFORM);
        stack.setRegion(REGION);
        stack.setPlatformvariant(PLATFORM);
        Credential credential = new Credential(PLATFORM, "", "", "", "acc");
        ExtendedCloudCredential extendedCred = new ExtendedCloudCredential(new CloudCredential(), PLATFORM, "", "", new ArrayList<>());
        CloudSubnet subnet1 = new CloudSubnet.Builder()
                .id(SUBNET_1)
                .name(SUBNET_1)
                .availabilityZone("")
                .cidr("10.1.0.0/24")
                .build();
        CloudSubnet subnet2 = new CloudSubnet.Builder()
                .id(SUBNET_2)
                .name(SUBNET_2)
                .availabilityZone("")
                .cidr("10.1.1.0/24")
                .build();
        CloudSubnet subnet3 = new CloudSubnet.Builder()
                .id("indifferent")
                .name("indifferent")
                .availabilityZone("")
                .cidr("10.1.2.0/24")
                .build();
        CloudNetwork cloudNetwork1 = new CloudNetwork("/rg1/" + NETWORK_ID, "/rg1/" + NETWORK_ID, Set.of(subnet1, subnet2, subnet3), Map.of());
        CloudNetwork cloudNetwork2 = new CloudNetwork("/rg2/" + NETWORK_ID, "/rg2/" + NETWORK_ID,
                Set.of(
                        new CloudSubnet.Builder()
                                .id(SUBNET_1)
                                .name(SUBNET_1)
                                .availabilityZone("")
                                .cidr("10.2.0.0/24")
                                .build(),
                        new CloudSubnet.Builder()
                                .id("test")
                                .name("test")
                                .build()), Map.of());
        Map<String, Set<CloudNetwork>> cloudNets = Map.of(REGION, Set.of(cloudNetwork1, cloudNetwork2));
        CloudNetworks cloudNetworks = new CloudNetworks(cloudNets);

        when(credentialService.getCredentialByEnvCrn(ENV_CRN)).thenReturn(credential);
        when(extendedCloudCredentialConverter.convert(credential)).thenReturn(extendedCred);
        when(cloudParameterService.getCloudNetworks(eq(extendedCred), eq(REGION), eq(PLATFORM), any())).thenReturn(cloudNetworks);
        when(networkFilterProviderMap.get(any())).thenReturn(null);

        Multimap<String, String> filteredSubnetWithCidr = underTest.getFilteredSubnetWithCidr(ENV_CRN, stack, NETWORK_ID, List.of(SUBNET_1, SUBNET_2));

        assertEquals(3, filteredSubnetWithCidr.size());
        assertEquals(2, filteredSubnetWithCidr.get(subnet1.getId()).size());
        assertTrue(filteredSubnetWithCidr.get(subnet1.getId()).contains(subnet1.getCidr()));
        assertTrue(filteredSubnetWithCidr.get(subnet1.getId()).contains("10.2.0.0/24"));
        assertEquals(1, filteredSubnetWithCidr.get(subnet2.getId()).size());
        assertEquals(subnet2.getCidr(), filteredSubnetWithCidr.get(subnet2.getId()).stream().findFirst().get());
    }

    @Test
    public void testAzureMultipleNetworkWithSameIds() {
        Stack stack = new Stack();
        stack.setCloudPlatform("AZURE");
        stack.setRegion("East US");
        stack.setPlatformvariant("AZURE");
        Credential credential = new Credential("AZURE", "", "", "", "acc");
        ExtendedCloudCredential extendedCred = new ExtendedCloudCredential(new CloudCredential(), "AZURE", "", "", new ArrayList<>());
        Set<CloudSubnet> subnets = Set.of(
                new CloudSubnet.Builder()
                        .id("CDPPROD-DataLake-GW")
                        .name("CDPPROD-DataLake-GW")
                        .cidr("10.278.245.32/28")
                        .privateSubnet(false)
                        .mapPublicIpOnLaunch(false)
                        .igwAvailable(false)
                        .build(),
                new CloudSubnet.Builder()
                        .id("CDPPROD-DataFlow")
                        .name("CDPPROD-DataFlow")
                        .cidr("10.278.245.64/28")
                        .privateSubnet(false)
                        .mapPublicIpOnLaunch(false)
                        .igwAvailable(false)
                        .build(),
                new CloudSubnet.Builder()
                        .id("CDPPROD-DataLake")
                        .name("CDPPROD-DataLake")
                        .cidr("10.278.245.0/27")
                        .privateSubnet(false)
                        .mapPublicIpOnLaunch(false)
                        .igwAvailable(false)
                        .build(),
                new CloudSubnet.Builder()
                        .id("CDPPROD-AirFlow")
                        .name("CDPPROD-AirFlow")
                        .cidr("10.278.245.48/28")
                        .privateSubnet(false)
                        .mapPublicIpOnLaunch(false)
                        .igwAvailable(false)
                        .build(),
                new CloudSubnet.Builder()
                        .id("CDPPROD-DataLake-NAT")
                        .name("CDPPROD-DataLake-NAT")
                        .cidr("10.278.245.248/29")
                        .privateSubnet(false)
                        .mapPublicIpOnLaunch(false)
                        .igwAvailable(false)
                        .build(),
                new CloudSubnet.Builder()
                        .id("CDPPROD-DataEngineering")
                        .name("CDPPROD-DataEngineering")
                        .cidr("10.278.245.128/27")
                        .privateSubnet(false)
                        .mapPublicIpOnLaunch(false)
                        .igwAvailable(false)
                        .build(),
                new CloudSubnet.Builder()
                        .id("CDPPROD-Postgre")
                        .name("CDPPROD-Postgre")
                        .cidr("10.278.245.160/27")
                        .privateSubnet(false)
                        .mapPublicIpOnLaunch(false)
                        .igwAvailable(false)
                        .build()
        );
        Map<String, Object> properties = new HashMap<>();
        properties.put("addressSpaces", List.of("10.278.245.0/24"));
        properties.put("resourceGroupName", "RSG_CDP_Prod");
        properties.put("dnsServerIPs", List.of());

        CloudNetwork cloudNetwork = new CloudNetwork(
                "VNET_CDP_Prod",
                "/subscriptions/40aa3b-bbb8-4ccb-ddfe-2ehhf839e52e/resourceGroups/RSG_CDP_Prod/providers/Microsoft.Network/virtualNetworks/VNET_CDP_Prod",
                subnets,
                properties
        );
        Map<String, Set<CloudNetwork>> cloudNets = Map.of("East US", Set.of(cloudNetwork));
        CloudNetworks cloudNetworks = new CloudNetworks(cloudNets);

        when(credentialService.getCredentialByEnvCrn(ENV_CRN)).thenReturn(credential);
        when(extendedCloudCredentialConverter.convert(credential)).thenReturn(extendedCred);
        when(cloudParameterService.getCloudNetworks(eq(extendedCred), eq("East US"), eq("AZURE"), any())).thenReturn(cloudNetworks);
        when(networkFilterProviderMap.get(any())).thenReturn(null);

        Multimap<String, String> filteredSubnetWithCidr = underTest.getFilteredSubnetWithCidr(ENV_CRN, stack, "VNET_CDP_Prod", List.of("CDPPROD-DataLake"));

        assertEquals(1, filteredSubnetWithCidr.size());
        Collection<String> subnetsResult = filteredSubnetWithCidr.get("CDPPROD-DataLake");
        assertEquals("10.278.245.0/27", subnetsResult.stream().findFirst().get());
    }

}