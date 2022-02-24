package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
        ExtendedCloudCredential extendedCred = new ExtendedCloudCredential(new CloudCredential(), PLATFORM, "", "", "");
        CloudSubnet subnet1 = new CloudSubnet(SUBNET_1, SUBNET_1, "", "10.1.0.0/24");
        CloudSubnet subnet2 = new CloudSubnet(SUBNET_2, SUBNET_2, "", "10.1.1.0/24");
        CloudSubnet subnet3 = new CloudSubnet("indifferent", "indifferent", "", "10.1.2.0/24");
        CloudNetwork cloudNetwork1 = new CloudNetwork(NETWORK_ID, NETWORK_ID, Set.of(subnet1, subnet2, subnet3), Map.of());
        CloudNetwork cloudNetwork2 = new CloudNetwork("other", "other",
                Set.of(new CloudSubnet(SUBNET_1, SUBNET_1), new CloudSubnet("test", "test")), Map.of());
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
        ExtendedCloudCredential extendedCred = new ExtendedCloudCredential(new CloudCredential(), PLATFORM, "", "", "");
        CloudSubnet subnet1 = new CloudSubnet(SUBNET_1, SUBNET_1, "", "10.1.0.0/24");
        CloudSubnet subnet2 = new CloudSubnet(SUBNET_2, SUBNET_2, "", "10.1.1.0/24");
        CloudSubnet subnet3 = new CloudSubnet("indifferent", "indifferent", "", "10.1.2.0/24");
        CloudNetwork cloudNetwork1 = new CloudNetwork("/rg1/" + NETWORK_ID, "/rg1/" + NETWORK_ID, Set.of(subnet1, subnet2, subnet3), Map.of());
        CloudNetwork cloudNetwork2 = new CloudNetwork("/rg2/" + NETWORK_ID, "/rg2/" + NETWORK_ID,
                Set.of(new CloudSubnet(SUBNET_1, SUBNET_1, "", "10.2.0.0/24"),
                        new CloudSubnet("test", "test")), Map.of());
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

}