package com.sequenceiq.redbeams.service.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.service.CredentialService;

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

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EnvironmentNetworkResponse environmentNetworkResponse;

    private CloudSubnet subnet1;

    private CloudSubnet subnet2;

    private Map<String, CloudSubnet> subnets;

    private Credential credential;

    @InjectMocks
    private SubnetListerService underTest;

    @Before
    public void setUp() {
        initMocks(this);

        when(detailedEnvironmentResponse.getCrn()).thenReturn(ENVIRONMENT_CRN);
        when(detailedEnvironmentResponse.getNetwork()).thenReturn(environmentNetworkResponse);

        subnet1 = new CloudSubnet(SUBNET_ID_1, "name1");
        subnet2 = new CloudSubnet(SUBNET_ID_2, "name2");
        subnets = Map.of(SUBNET_ID_1, subnet1, SUBNET_ID_2, subnet2);
    }

    @Test
    public void testListSubnetsAws() {
        when(environmentNetworkResponse.getSubnetMetas()).thenReturn(subnets);
        credential = new Credential(CREDENTIAL_CRN, CREDENTIAL_NAME, CREDENTIAL_ATTRIBUTES);
        when(credentialService.getCredentialByEnvCrn(ENVIRONMENT_CRN)).thenReturn(credential);

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
        credential = new Credential(CREDENTIAL_CRN, CREDENTIAL_NAME, CREDENTIAL_ATTRIBUTES, azure);
        when(credentialService.getCredentialByEnvCrn(ENVIRONMENT_CRN)).thenReturn(credential);

        List<CloudSubnet> subnets = underTest.listSubnets(detailedEnvironmentResponse, CloudPlatform.AZURE);

        assertEquals(2, subnets.size());
        Set<String> ids = subnets.stream().map(CloudSubnet::getId).collect(Collectors.toSet());
        assertTrue(ids.contains(SubnetListerService.expandAzureResourceId(subnet1, detailedEnvironmentResponse, SUBSCRIPTION_ID).getId()));
        assertTrue(ids.contains(SubnetListerService.expandAzureResourceId(subnet2, detailedEnvironmentResponse, SUBSCRIPTION_ID).getId()));
    }

    @Test
    public void testExpandAzureResourceId() {
        when(environmentNetworkResponse.getAzure().getResourceGroupName()).thenReturn(RESOURCE_GROUP);
        when(environmentNetworkResponse.getAzure().getNetworkId()).thenReturn(NETWORK_ID);

        CloudSubnet expandedSubnet = SubnetListerService.expandAzureResourceId(subnet1, detailedEnvironmentResponse, SUBSCRIPTION_ID);

        String expectedId = String.format("/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/virtualNetworks/%s/subnets/%s",
            SUBSCRIPTION_ID, RESOURCE_GROUP, NETWORK_ID, subnet1.getId());
        assertEquals(expectedId, expandedSubnet.getId());
    }
}
