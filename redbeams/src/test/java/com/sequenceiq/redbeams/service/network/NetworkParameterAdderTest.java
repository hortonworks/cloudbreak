package com.sequenceiq.redbeams.service.network;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.collection.IsMapContaining;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.ServiceEndpointCreationToEndpointTypeConverter;
import com.sequenceiq.common.model.PrivateEndpointType;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams.EnvironmentNetworkAwsParamsBuilder;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.redbeams.domain.stack.DBStack;

public class NetworkParameterAdderTest {

    private static final String TEST_VPC_CIDR = "1.2.3.4/16";

    private static final String TEST_VPC_ID = "vpcId";

    @Mock
    private ServiceEndpointCreationToEndpointTypeConverter serviceEndpointCreationToEndpointTypeConverter;

    @Mock
    private SubnetListerService subnetListerService;

    @Mock
    private SubnetChooserService subnetChooserService;

    @InjectMocks
    private final NetworkParameterAdder underTest = new NetworkParameterAdder();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testAddNetworkParametersWhenAws() {
        Map<String, Object> parameters = new HashMap<>();
        List<String> subnetIds = List.of("subnet1", "subnet2");

        parameters = underTest.addSubnetIds(parameters, subnetIds, List.of(), CloudPlatform.AWS);

        assertThat(parameters, IsMapContaining.hasEntry(NetworkParameterAdder.SUBNET_ID, String.join(",", subnetIds)));
    }

    @Test
    public void testAddParametersWhenAws() {
        Map<String, Object> parameters = new HashMap<>();
        DBStack dbStack = new DBStack();
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withCloudPlatform(CloudPlatform.AWS.name())
                .withNetwork(EnvironmentNetworkResponse.builder()
                        .withAws(EnvironmentNetworkAwsParamsBuilder.anEnvironmentNetworkAwsParams().withVpcId(TEST_VPC_ID).build())
                        .withNetworkCidr(TEST_VPC_CIDR)
                        .build())
                .build();

        parameters = underTest.addParameters(parameters, environment, CloudPlatform.AWS, dbStack);

        assertThat(parameters, IsMapContaining.hasEntry(NetworkParameterAdder.VPC_ID, TEST_VPC_ID));
        assertThat(parameters, IsMapContaining.hasEntry(NetworkParameterAdder.VPC_CIDR, TEST_VPC_CIDR));
    }

    @Test
    public void testAddParametersWhenAzure() {
        Map<String, Object> parameters = new HashMap<>();
        DBStack dbStack = new DBStack();
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withCloudPlatform(CloudPlatform.AZURE.name())
                .withNetwork(EnvironmentNetworkResponse.builder()
                        .withSubnetMetas(Map.of())
                        .withAzure(
                                EnvironmentNetworkAzureParams.EnvironmentNetworkAzureParamsBuilder.anEnvironmentNetworkAzureParams()
                                        .withResourceGroupName("myResourceGroup")
                                        .withNetworkId("networkId")
                                .build()
                        )
                        .build())
                .build();
        CloudSubnet subnetForPrivateEndpoint = new CloudSubnet("mySubnet", "");
        when(subnetListerService.getAzureSubscriptionId(any())).thenReturn("mySubscription");
        when(subnetChooserService.chooseSubnetForPrivateEndpoint(any(), any(), anyBoolean())).thenReturn(List.of(subnetForPrivateEndpoint));
        when(serviceEndpointCreationToEndpointTypeConverter.convert(any(), any())).thenReturn(PrivateEndpointType.USE_PRIVATE_ENDPOINT);
        when(subnetListerService.expandAzureResourceId(any(), any(), anyString())).thenCallRealMethod();

        parameters = underTest.addParameters(parameters, environment, CloudPlatform.AZURE, dbStack);

        assertThat(parameters, IsMapContaining.hasEntry(NetworkParameterAdder.ENDPOINT_TYPE, PrivateEndpointType.USE_PRIVATE_ENDPOINT));
        assertThat(parameters, IsMapContaining.hasEntry(NetworkParameterAdder.SUBNET_FOR_PRIVATE_ENDPOINT,
                "/subscriptions/mySubscription/resourceGroups/myResourceGroup/providers/Microsoft.Network/virtualNetworks/networkId/subnets/mySubnet"));
    }
}
