package com.sequenceiq.redbeams.service.network;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.ServiceEndpointCreationToEndpointTypeConverter;
import com.sequenceiq.common.model.PrivateEndpointType;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams.EnvironmentNetworkAwsParamsBuilder;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkGcpParams;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.redbeams.domain.stack.DBStack;

@ExtendWith(MockitoExtension.class)
class NetworkParameterAdderTest {

    private static final String TEST_VPC_CIDR = "1.2.3.4/16";

    private static final String TEST_VPC_ID = "vpcId";

    private static final String VPC_ID = "vpcId";

    private static final String VPC_CIDR = "vpcCidr";

    private static final String SHARED_PROJECT_ID = "sharedProjectId";

    private static final String VPC_CIDRS = "vpcCidrs";

    private static final String SUBNET_ID = "subnetId";

    private static final String AVAILABILITY_ZONE = "availabilityZone";

    private static final String ENDPOINT_TYPE = "endpointType";

    private static final String SUBNET_FOR_PRIVATE_ENDPOINT = "subnetForPrivateEndpoint";

    private static final String EXISTING_DATABASE_PRIVATE_DNS_ZONE_ID = "existingDatabasePrivateDnsZoneId";

    private static final String FLEXIBLE_SERVER_DELEGATED_SUBNET_ID = "flexibleServerDelegatedSubnetId";

    private static final String SUBNETS = "subnets";

    private static final String SUBNET_RESOURCE_ID =
            "/subscriptions/mySubscription/resourceGroups/myResourceGroup/providers/Microsoft.Network/virtualNetworks/networkId/subnets/mySubnet";

    @Mock
    private ServiceEndpointCreationToEndpointTypeConverter serviceEndpointCreationToEndpointTypeConverter;

    @Mock
    private SubnetListerService subnetListerService;

    @Mock
    private SubnetChooserService subnetChooserService;

    @InjectMocks
    private final NetworkParameterAdder underTest = new NetworkParameterAdder();

    @Test
    void testAddSubnetIdsWhenAws() {
        Map<String, Object> parameters = underTest.addSubnetIds(List.of("subnet1", "subnet2"), List.of("az1", "az2"), CloudPlatform.AWS);

        assertThat(parameters, IsMapContaining.hasEntry(SUBNET_ID, "subnet1,subnet2"));
        assertThat(parameters, IsMapContaining.hasEntry(AVAILABILITY_ZONE, "az1,az2"));
    }

    @Test
    void testAddSubnetIdsWhenAzure() {
        Map<String, Object> parameters = underTest.addSubnetIds(List.of("subnet1", "subnet2"), List.of(), CloudPlatform.AZURE);

        assertThat(parameters, IsMapContaining.hasEntry(SUBNETS, "subnet1,subnet2"));
    }

    @Test
    void testAddSubnetIdsWhenGcp() {
        Map<String, Object> parameters = underTest.addSubnetIds(List.of("subnet1", "subnet2"), List.of("az1", "az2"), CloudPlatform.GCP);

        assertThat(parameters, IsMapContaining.hasEntry(SUBNET_ID, "subnet1,subnet2"));
        assertThat(parameters, IsMapContaining.hasEntry(AVAILABILITY_ZONE, "az1,az2"));
    }

    @Test
    void testAddParametersWhenAws() {
        DBStack dbStack = new DBStack();
        dbStack.setCloudPlatform("AWS");
        DetailedEnvironmentResponse environment = getAwsDetailedEnvironmentResponse();

        Map<String, Object> parameters = underTest.addParameters(environment, dbStack);

        assertThat(parameters, IsMapContaining.hasEntry(VPC_ID, TEST_VPC_ID));
        assertThat(parameters, IsMapContaining.hasEntry(VPC_CIDR, TEST_VPC_CIDR));
        assertThat(parameters, IsMapContaining.hasEntry(VPC_CIDRS, Set.of(TEST_VPC_CIDR)));
    }

    @Test
    void testAddParametersWhenAzure() {
        DBStack dbStack = new DBStack();
        dbStack.setCloudPlatform("AZURE");
        DetailedEnvironmentResponse environment = getAzureDetailedEnvironmentResponse();
        environment.getNetwork().setSubnetMetas(Map.of("mySubnet",
                new CloudSubnet.Builder()
                .id("mySubnet")
                .name("")
                .build()));
        when(subnetListerService.getAzureSubscriptionId(any())).thenReturn("mySubscription");
        when(serviceEndpointCreationToEndpointTypeConverter.convert(any(), any())).thenReturn(PrivateEndpointType.USE_PRIVATE_ENDPOINT);
        CloudSubnet cloudSubnet = new CloudSubnet.Builder()
                .id(SUBNET_RESOURCE_ID)
                .availabilityZone(SUBNET_ID)
                .build();
        when(subnetListerService.expandAzureResourceId(any(CloudSubnet.class), any(), anyString())).thenReturn(cloudSubnet);

        Map<String, Object> parameters = underTest.addParameters(environment, dbStack);

        assertThat(parameters, IsMapContaining.hasEntry(ENDPOINT_TYPE, PrivateEndpointType.USE_PRIVATE_ENDPOINT));
        assertThat(parameters, IsMapContaining.hasEntry(SUBNET_FOR_PRIVATE_ENDPOINT,
                SUBNET_RESOURCE_ID));
        assertThat(parameters, IsMapContaining.hasEntry(EXISTING_DATABASE_PRIVATE_DNS_ZONE_ID, "databasePrivateDsZoneId"));
    }

    @Test
    void testAddParametersWhenAzureWithFlexibleDelegated() {
        DBStack dbStack = new DBStack();
        dbStack.setCloudPlatform("AZURE");
        Set<String> delegatedSubnetIds = Set.of("flexSubnetSmall", "flexSubnetLarge", "flexWithoutCidr");
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withCloudPlatform(CloudPlatform.AZURE.name())
                .withNetwork(EnvironmentNetworkResponse.builder()
                        .withSubnetMetas(Map.of("mySubnet", new CloudSubnet.Builder()
                                .id("mySubnet")
                                .name("")
                                .build()))
                        .withAzure(
                                EnvironmentNetworkAzureParams.EnvironmentNetworkAzureParamsBuilder.anEnvironmentNetworkAzureParams()
                                        .withResourceGroupName("myResourceGroup")
                                        .withNetworkId("networkId")
                                        .withDatabasePrivateDnsZoneId("databasePrivateDsZoneId")
                                        .withFlexibleServerSubnetIds(delegatedSubnetIds)
                                        .build()
                        )
                        .build())
                .build();
        CloudSubnet subnetForPrivateEndpoint = new CloudSubnet.Builder()
                .id("mySubnet")
                .name("")
                .build();
        when(subnetListerService.getAzureSubscriptionId(any())).thenReturn("mySubscription");
        when(serviceEndpointCreationToEndpointTypeConverter.convert(any(), any())).thenReturn(PrivateEndpointType.USE_PRIVATE_ENDPOINT);
        CloudSubnet cloudSubnet = new CloudSubnet.Builder()
                .id(SUBNET_RESOURCE_ID)
                .name(SUBNET_ID)
                .build();
        when(subnetListerService.expandAzureResourceId(subnetForPrivateEndpoint, environment, "mySubscription")).thenReturn(cloudSubnet);
        CloudSubnet cloudSubnetLarge = new CloudSubnet.Builder()
                .id("flexSubnetLarge")
                .name("flexSubnetLarge")
                .availabilityZone("az")
                .cidr("10.3.0.0/16")
                .build();
        when(subnetListerService.expandAzureResourceId(cloudSubnetLarge, environment, "mySubscription"))
                .thenReturn(new CloudSubnet.Builder()
                        .id("flexSubnetLargeExpanded")
                        .name("flexSubnetLargeExpanded")
                        .build()
                );
        when(subnetListerService.fetchNetworksFiltered(dbStack, delegatedSubnetIds))
                .thenReturn(Set.of(
                        new CloudSubnet.Builder()
                                .id("flexSubnetSmall")
                                .name("flexSubnetSmall")
                                .availabilityZone("az")
                                .cidr("192.168.1.0/24")
                                .build(),
                        cloudSubnetLarge,
                        new CloudSubnet.Builder()
                                .id("flexWithoutCidr")
                                .name("flexWithoutCidr")
                                .build()));

        Map<String, Object> parameters = underTest.addParameters(environment, dbStack);

        assertThat(parameters, IsMapContaining.hasEntry(ENDPOINT_TYPE, PrivateEndpointType.USE_PRIVATE_ENDPOINT));
        assertThat(parameters, IsMapContaining.hasEntry(SUBNET_FOR_PRIVATE_ENDPOINT,
                SUBNET_RESOURCE_ID));
        assertThat(parameters, IsMapContaining.hasEntry(EXISTING_DATABASE_PRIVATE_DNS_ZONE_ID, "databasePrivateDsZoneId"));
        assertThat(parameters, IsMapContaining.hasEntry(FLEXIBLE_SERVER_DELEGATED_SUBNET_ID, "flexSubnetLargeExpanded"));
    }

    @Test
    void testAddParametersWhenGcp() {
        DBStack dbStack = new DBStack();
        dbStack.setCloudPlatform("GCP");
        DetailedEnvironmentResponse environment = getGcpDetailedEnvironmentResponse();

        Map<String, Object> parameters = underTest.addParameters(environment, dbStack);

        assertThat(parameters, IsMapContaining.hasEntry(SHARED_PROJECT_ID, "sharedProjectId"));
    }

    private DetailedEnvironmentResponse getAwsDetailedEnvironmentResponse() {
        return DetailedEnvironmentResponse.builder()
                .withCloudPlatform(CloudPlatform.AWS.name())
                .withNetwork(EnvironmentNetworkResponse.builder()
                        .withAws(EnvironmentNetworkAwsParamsBuilder.anEnvironmentNetworkAwsParams().withVpcId(TEST_VPC_ID).build())
                        .withNetworkCidr(TEST_VPC_CIDR)
                        .withNetworkCidrs(Set.of(TEST_VPC_CIDR))
                        .build())
                .build();
    }

    private DetailedEnvironmentResponse getAzureDetailedEnvironmentResponse() {
        return DetailedEnvironmentResponse.builder()
                .withCloudPlatform(CloudPlatform.AZURE.name())
                .withNetwork(EnvironmentNetworkResponse.builder()
                        .withSubnetMetas(Map.of())
                        .withAzure(
                                EnvironmentNetworkAzureParams.EnvironmentNetworkAzureParamsBuilder.anEnvironmentNetworkAzureParams()
                                        .withResourceGroupName("myResourceGroup")
                                        .withNetworkId("networkId")
                                        .withDatabasePrivateDnsZoneId("databasePrivateDsZoneId")
                                        .build()
                        )
                        .build())
                .build();
    }

    private DetailedEnvironmentResponse getGcpDetailedEnvironmentResponse() {
        return DetailedEnvironmentResponse.builder()
                .withCloudPlatform(CloudPlatform.GCP.name())
                .withNetwork(EnvironmentNetworkResponse.builder()
                        .withGcp(EnvironmentNetworkGcpParams.EnvironmentNetworkGcpParamsBuilder.anEnvironmentNetworkGcpParamsBuilder()
                                .withSharedProjectId("sharedProjectId")
                                .build())
                        .withNetworkCidr(TEST_VPC_CIDR)
                        .build())
                .build();
    }

}
