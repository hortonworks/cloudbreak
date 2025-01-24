package com.sequenceiq.redbeams.service.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.redbeams.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.service.CredentialService;

@ExtendWith(MockitoExtension.class)
public class SubnetListerServiceTest {

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String SUBNET_ID_1 = "subnet-1";

    private static final String SUBNET_ID_2 = "subnet-2";

    private static final String RESOURCE_GROUP = "rg";

    private static final String NETWORK_ID = "vnet";

    private static final String CREDENTIAL_CRN = "mycredsCrn";

    private static final String CREDENTIAL_NAME = "mycreds";

    private static final String CREDENTIAL_ATTRIBUTES = "mycredsAttributes";

    private static final String SUBSCRIPTION_ID = "subscriptionId";

    @Mock
    private CredentialService credentialService;

    @Mock
    private DetailedEnvironmentResponse detailedEnvironmentResponse;

    @Mock
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Mock
    private CloudParameterService cloudParameterService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EnvironmentNetworkResponse environmentNetworkResponse;

    private final CloudSubnet subnet1 = new CloudSubnet.Builder()
            .id(SUBNET_ID_1)
            .availabilityZone("name1")
            .build();

    private final CloudSubnet subnet2 = new CloudSubnet.Builder()
            .id(SUBNET_ID_2)
            .availabilityZone("name2")
            .build();

    private final Map<String, CloudSubnet> subnets = Map.of(SUBNET_ID_1, subnet1, SUBNET_ID_2, subnet2);

    private final Credential credential = new Credential(CREDENTIAL_CRN, CREDENTIAL_NAME, CREDENTIAL_ATTRIBUTES, "acc");

    @InjectMocks
    private SubnetListerService underTest;

    @BeforeEach
    public void setUp() {
        lenient().when(detailedEnvironmentResponse.getCrn()).thenReturn(ENVIRONMENT_CRN);
        lenient().when(detailedEnvironmentResponse.getNetwork()).thenReturn(environmentNetworkResponse);
    }

    @Test
    public void testListSubnetsAws() {
        when(environmentNetworkResponse.getSubnetMetas()).thenReturn(subnets);
        when(environmentNetworkResponse.getCbSubnets()).thenReturn(subnets);

        List<CloudSubnet> subnets = underTest.listSubnets(detailedEnvironmentResponse, CloudPlatform.AWS);

        assertEquals(2, subnets.size());
        assertTrue(subnets.contains(subnet1));
        assertTrue(subnets.contains(subnet2));
    }

    @Test
    public void testListSubnetsAzure() {
        when(environmentNetworkResponse.getSubnetMetas()).thenReturn(subnets);
        when(environmentNetworkResponse.getAzure().getResourceGroupName()).thenReturn(RESOURCE_GROUP);
        when(environmentNetworkResponse.getAzure().getNetworkId()).thenReturn(NETWORK_ID);
        Credential.AzureParameters azure = new Credential.AzureParameters(SUBSCRIPTION_ID);
        Credential azureCredential = new Credential(CREDENTIAL_CRN, CREDENTIAL_NAME, CREDENTIAL_ATTRIBUTES, azure, "acc");
        when(credentialService.getCredentialByEnvCrn(ENVIRONMENT_CRN)).thenReturn(azureCredential);

        List<CloudSubnet> subnets = underTest.listSubnets(detailedEnvironmentResponse, CloudPlatform.AZURE);

        assertEquals(2, subnets.size());
        Set<String> ids = subnets.stream().map(CloudSubnet::getId).collect(Collectors.toSet());
        assertTrue(ids.contains(formatAzureResourceId(SUBSCRIPTION_ID, RESOURCE_GROUP, NETWORK_ID, SUBNET_ID_1)));
        assertTrue(ids.contains(formatAzureResourceId(SUBSCRIPTION_ID, RESOURCE_GROUP, NETWORK_ID, SUBNET_ID_2)));
    }

    @Test
    public void testExpandAzureResourceId() {
        when(environmentNetworkResponse.getAzure().getResourceGroupName()).thenReturn(RESOURCE_GROUP);
        when(environmentNetworkResponse.getAzure().getNetworkId()).thenReturn(NETWORK_ID);

        CloudSubnet expandedSubnet = underTest.expandAzureResourceId(subnet1, detailedEnvironmentResponse, SUBSCRIPTION_ID);

        String expectedId = formatAzureResourceId(SUBSCRIPTION_ID, RESOURCE_GROUP, NETWORK_ID, subnet1.getId());
        assertEquals(expectedId, expandedSubnet.getId());
    }

    @Test
    public void testExpandAzureResourceIdWithResourceId() {
        String subnetResourceId = formatAzureResourceId(SUBSCRIPTION_ID, RESOURCE_GROUP, NETWORK_ID, SUBNET_ID_1);
        CloudSubnet cloudSubnet = new CloudSubnet.Builder()
                .id(subnetResourceId)
                .availabilityZone("name1")
                .build();

        CloudSubnet expandedSubnet = underTest.expandAzureResourceId(cloudSubnet, detailedEnvironmentResponse, SUBSCRIPTION_ID);

        assertEquals(subnetResourceId, expandedSubnet.getId());
    }

    @Test
    void testFetchNetworksFilteredShortId() {
        DBStack dbStack = new DBStack();
        dbStack.setEnvironmentId(ENVIRONMENT_CRN);
        dbStack.setCloudPlatform("AZURE");
        dbStack.setRegion("sampleRegion");
        dbStack.setPlatformVariant("AZURE");

        when(credentialService.getCredentialByEnvCrn(ENVIRONMENT_CRN)).thenReturn(credential);

        ExtendedCloudCredential extendedCloudCredential = mock(ExtendedCloudCredential.class);
        when(extendedCloudCredentialConverter.convert(credential, dbStack.getCloudPlatform())).thenReturn(extendedCloudCredential);

        CloudNetworks cloudNetworks = new CloudNetworks();
        cloudNetworks.setCloudNetworkResponses(Map.of("asdf", Set.of(new CloudNetwork("asdf", "asdf", Set.of(subnet1, subnet2), Map.of()))));
        when(cloudParameterService.getCloudNetworks(eq(extendedCloudCredential), eq(dbStack.getRegion()), anyString(), anyMap()))
                .thenReturn(cloudNetworks);

        List<String> subnetIds = Arrays.asList(SUBNET_ID_1, SUBNET_ID_2);

        Set<CloudSubnet> result = underTest.fetchNetworksFiltered(dbStack, subnetIds);

        assertEquals(2, result.size());
    }

    @Test
    void testFetchNetworksFilteredLongId() {
        DBStack dbStack = new DBStack();
        dbStack.setEnvironmentId(ENVIRONMENT_CRN);
        dbStack.setCloudPlatform("AZURE");
        dbStack.setRegion("sampleRegion");
        dbStack.setPlatformVariant("AZURE");

        when(credentialService.getCredentialByEnvCrn(ENVIRONMENT_CRN)).thenReturn(credential);

        ExtendedCloudCredential extendedCloudCredential = mock(ExtendedCloudCredential.class);
        when(extendedCloudCredentialConverter.convert(credential, dbStack.getCloudPlatform())).thenReturn(extendedCloudCredential);

        CloudNetworks cloudNetworks = new CloudNetworks();
        cloudNetworks.setCloudNetworkResponses(Map.of("asdf", Set.of(new CloudNetwork("asdf", "asdf", Set.of(subnet1, subnet2), Map.of()))));
        when(cloudParameterService.getCloudNetworks(eq(extendedCloudCredential), eq(dbStack.getRegion()), anyString(), anyMap()))
                .thenReturn(cloudNetworks);

        List<String> subnetIds = Arrays.asList("Microsoft/VPC/NETWORK/WHATEVER/" + SUBNET_ID_1, "2");

        Set<CloudSubnet> result = underTest.fetchNetworksFiltered(dbStack, subnetIds);

        assertEquals(1, result.size());
        assertTrue(result.stream().allMatch(cloudSubnet -> cloudSubnet.getId().equals(SUBNET_ID_1)));
    }

    private String formatAzureResourceId(String subscription, String resourceGroup, String networkId, String subnetId) {
        return String.format("/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/virtualNetworks/%s/subnets/%s",
                subscription, resourceGroup, networkId, subnetId);
    }
}
