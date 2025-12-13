package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.model.PrivateEndpointType;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import software.amazon.awssdk.services.ec2.model.DescribeVpcEndpointServicesResponse;
import software.amazon.awssdk.services.ec2.model.ServiceDetail;

@ExtendWith(MockitoExtension.class)
public class AwsNetworkCfTemplateProviderTest {

    private static final String VPC_CIDR = "0.0.0.0/16";

    private static final String TEMPLATE_PATH = "templates/aws-cf-network.ftl";

    @InjectMocks
    private AwsNetworkCfTemplateProvider underTest;

    @Mock
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Mock
    private AwsCloudFormationClient awsClient;

    private static Stream<Arguments> privateSubnetArguments() {
        return Stream.of(
                Arguments.of("src/test/resources/json/aws-cf-network-privatesubnet-onlygatewayvpcendpoints.json", List.of("gateway1", "gateway2"),
                        List.of("interface1", "interface2"), DescribeVpcEndpointServicesResponse.builder().build()),
                Arguments.of("src/test/resources/json/aws-cf-network-privatesubnet-onlyinterfacevpcendpoints.json", List.of(),
                        List.of("interface1", "interface2"), createDescribeVpcEndpointServicesResult("interface1", "interface2")),
                Arguments.of("src/test/resources/json/aws-cf-network-privatesubnet-novpcendpoints.json", List.of(), List.of("interface1", "interface2"),
                        DescribeVpcEndpointServicesResponse.builder().build()),
                Arguments.of("src/test/resources/json/aws-cf-network-privatesubnet-vpcendpoints.json", List.of("gateway1", "gateway2"),
                        List.of("interface1", "interface2"), createDescribeVpcEndpointServicesResult("interface1", "interface2")),
                Arguments.of("src/test/resources/json/aws-cf-network-privatesubnet-vpcendpoints-templatenamechange.json", List.of("gateway-1", "gateway.2"),
                        List.of("interface-1", "interface.2"), createDescribeVpcEndpointServicesResult("interface-1", "interface.2")),
                Arguments.of("src/test/resources/json/aws-cf-network-privatesubnet-missingvpcendpoints.json", List.of("gateway1", "gateway2"),
                        List.of("interface1", "interface2"), createDescribeVpcEndpointServicesResult("interface1"))
        );
    }

    private static List<SubnetRequest> createPrivateAndPublicSubnetRequestList() {
        SubnetRequest subnetRequest1 = new SubnetRequest();
        subnetRequest1.setPublicSubnetCidr("2.2.2.2/24");
        subnetRequest1.setIndex(0);
        subnetRequest1.setSubnetGroup(0);
        subnetRequest1.setAvailabilityZone("az1");

        SubnetRequest subnetRequest2 = new SubnetRequest();
        subnetRequest2.setPublicSubnetCidr("2.2.2.2/24");
        subnetRequest2.setIndex(1);
        subnetRequest2.setSubnetGroup(1);
        subnetRequest2.setAvailabilityZone("az2");

        SubnetRequest subnetRequest3 = new SubnetRequest();
        subnetRequest3.setPrivateSubnetCidr("2.2.2.2/24");
        subnetRequest3.setIndex(2);
        subnetRequest3.setSubnetGroup(2);
        subnetRequest3.setAvailabilityZone("az1");

        SubnetRequest subnetRequest4 = new SubnetRequest();
        subnetRequest4.setPrivateSubnetCidr("2.2.2.2/24");
        subnetRequest4.setIndex(3);
        subnetRequest4.setSubnetGroup(3);
        subnetRequest4.setAvailabilityZone("az2");

        return List.of(subnetRequest1, subnetRequest2, subnetRequest3, subnetRequest4);
    }

    private static List<SubnetRequest> createPublicSubnetRequestList() {
        SubnetRequest subnetRequest1 = new SubnetRequest();
        subnetRequest1.setPublicSubnetCidr("2.2.2.2/24");
        subnetRequest1.setIndex(0);
        subnetRequest1.setSubnetGroup(0);
        subnetRequest1.setAvailabilityZone("az1");

        SubnetRequest subnetRequest2 = new SubnetRequest();
        subnetRequest2.setPublicSubnetCidr("2.2.2.2/24");
        subnetRequest2.setIndex(1);
        subnetRequest2.setSubnetGroup(1);
        subnetRequest2.setAvailabilityZone("az2");

        SubnetRequest subnetRequest3 = new SubnetRequest();
        subnetRequest3.setPublicSubnetCidr("2.2.2.2/24");
        subnetRequest3.setIndex(2);
        subnetRequest3.setSubnetGroup(2);
        subnetRequest3.setAvailabilityZone("az3");

        SubnetRequest subnetRequest4 = new SubnetRequest();
        subnetRequest4.setPublicSubnetCidr("2.2.2.2/24");
        subnetRequest4.setIndex(3);
        subnetRequest4.setSubnetGroup(3);
        subnetRequest4.setAvailabilityZone("az4");

        return List.of(subnetRequest1, subnetRequest2, subnetRequest3, subnetRequest4);
    }

    private static DescribeVpcEndpointServicesResponse createDescribeVpcEndpointServicesResult(String... services) {
        List<ServiceDetail> serviceDetails = new ArrayList<>();
        for (String service : services) {
            ServiceDetail serviceDetail = ServiceDetail.builder()
                    .serviceName(String.format(AwsNetworkCfTemplateProvider.VPC_INTERFACE_SERVICE_ENDPOINT_NAME_PATTERN, "region", service))
                    .availabilityZones(List.of("az1", "az2"))
                    .build();
            serviceDetails.add(serviceDetail);
        }
        return DescribeVpcEndpointServicesResponse.builder()
                .serviceDetails(serviceDetails)
                .build();
    }

    private static DescribeVpcEndpointServicesResponse createDescribeVpcEndpointServicesResultWithDifferentAzs() {
        List<ServiceDetail> serviceDetails = new ArrayList<>();
        ServiceDetail serviceDetail1 = ServiceDetail.builder()
                .serviceName(String.format(AwsNetworkCfTemplateProvider.VPC_INTERFACE_SERVICE_ENDPOINT_NAME_PATTERN, "region", "interface1"))
                .availabilityZones(List.of("az1"))
                .build();
        serviceDetails.add(serviceDetail1);
        ServiceDetail serviceDetail2 = ServiceDetail.builder()
                .serviceName(String.format(AwsNetworkCfTemplateProvider.VPC_INTERFACE_SERVICE_ENDPOINT_NAME_PATTERN, "region", "interface2"))
                .availabilityZones(List.of("az2", "az3"))
                .build();
        serviceDetails.add(serviceDetail2);
        ServiceDetail serviceDetail3 = ServiceDetail.builder()
                .serviceName(String.format(AwsNetworkCfTemplateProvider.VPC_INTERFACE_SERVICE_ENDPOINT_NAME_PATTERN, "region", "interface3"))
                .availabilityZones(List.of("az5", "az6"))
                .build();
        serviceDetails.add(serviceDetail3);
        return DescribeVpcEndpointServicesResponse.builder()
                .serviceDetails(serviceDetails)
                .build();
    }

    @BeforeEach
    public void before() throws IOException, TemplateException {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        ReflectionTestUtils.setField(underTest, "freemarkerConfiguration", factoryBean.getObject());
        ReflectionTestUtils.setField(underTest, "cloudFormationNetworkTemplatePath", TEMPLATE_PATH);
        ReflectionTestUtils.setField(underTest, "gatewayServices", List.of("gateway1", "gateway2"));
        ReflectionTestUtils.setField(underTest, "interfaceServices", List.of("interface1", "interface2"));
    }

    @ParameterizedTest
    @MethodSource("privateSubnetArguments")
    public void testProvideWhenPrivateSubnetCreationEnabled(String expectedTemplate, List<String> gatewayServices, List<String> interfaceServices,
            DescribeVpcEndpointServicesResponse describeVpcEndpointServicesResult) throws IOException, TemplateException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedJson = objectMapper.readTree(new File(expectedTemplate));

        when(freeMarkerTemplateUtils.processTemplateIntoString(any(), any())).thenCallRealMethod();
        AmazonEc2Client ec2Client = mock(AmazonEc2Client.class);
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(ec2Client);
        when(ec2Client.describeVpcEndpointServices()).thenReturn(describeVpcEndpointServicesResult);
        NetworkCreationRequest networkCreationRequest = createNetworkRequest(true, PrivateEndpointType.USE_VPC_ENDPOINT);
        List<SubnetRequest> subnetRequestList = createPrivateAndPublicSubnetRequestList();

        ReflectionTestUtils.setField(underTest, "gatewayServices", gatewayServices);
        ReflectionTestUtils.setField(underTest, "interfaceServices", interfaceServices);

        String actual = underTest.provide(networkCreationRequest, subnetRequestList);

        JsonNode json = objectMapper.readTree(actual);
        assertEquals(expectedJson, json);
        verify(freeMarkerTemplateUtils).processTemplateIntoString(any(Template.class), anyMap());
    }

    @Test
    public void testProvideWhenPrivateSubnetsButNoVpcEndpointsConfigured() throws IOException, TemplateException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedJson = objectMapper.readTree(new File("src/test/resources/json/aws-cf-network-privatesubnet-novpcendpoints.json"));

        when(freeMarkerTemplateUtils.processTemplateIntoString(any(), any())).thenCallRealMethod();
        ReflectionTestUtils.setField(underTest, "gatewayServices", List.of());
        ReflectionTestUtils.setField(underTest, "interfaceServices", List.of());
        NetworkCreationRequest networkCreationRequest = createNetworkRequest(true, PrivateEndpointType.USE_VPC_ENDPOINT);
        List<SubnetRequest> subnetRequestList = createPrivateAndPublicSubnetRequestList();

        String actual = underTest.provide(networkCreationRequest, subnetRequestList);

        JsonNode json = objectMapper.readTree(actual);
        assertEquals(expectedJson, json);
        verify(freeMarkerTemplateUtils).processTemplateIntoString(any(Template.class), anyMap());
    }

    @Test
    public void testProvideWhenPrivateSubnetsAndInterfaceServicesAreDisabled() throws IOException, TemplateException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedJson = objectMapper.readTree(new File("src/test/resources/json/aws-cf-network-privatesubnet-onlygatewayvpcendpoints.json"));

        when(freeMarkerTemplateUtils.processTemplateIntoString(any(), any())).thenCallRealMethod();
        NetworkCreationRequest networkCreationRequest = createNetworkRequest(true, PrivateEndpointType.NONE);
        List<SubnetRequest> subnetRequestList = createPrivateAndPublicSubnetRequestList();

        String actual = underTest.provide(networkCreationRequest, subnetRequestList);

        JsonNode json = objectMapper.readTree(actual);
        assertEquals(expectedJson, json);
        verify(freeMarkerTemplateUtils).processTemplateIntoString(any(Template.class), anyMap());
    }

    @Test
    public void testProvideWhenPrivateSubnetsAreDisabledAndInterfaceServicesWithDifferentAzs() throws IOException, TemplateException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedJson = objectMapper.readTree(new File("src/test/resources/json/aws-cf-network-publicsubnet-vpcendpoints-differentazs.json"));

        when(freeMarkerTemplateUtils.processTemplateIntoString(any(), any())).thenCallRealMethod();
        AmazonEc2Client ec2Client = mock(AmazonEc2Client.class);
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(ec2Client);
        when(ec2Client.describeVpcEndpointServices()).thenReturn(createDescribeVpcEndpointServicesResultWithDifferentAzs());
        NetworkCreationRequest networkCreationRequest = createNetworkRequest(false, PrivateEndpointType.USE_VPC_ENDPOINT);
        List<SubnetRequest> subnetRequestList = createPublicSubnetRequestList();

        String actual = underTest.provide(networkCreationRequest, subnetRequestList);

        JsonNode json = objectMapper.readTree(actual);
        assertEquals(expectedJson, json);
        verify(freeMarkerTemplateUtils).processTemplateIntoString(any(Template.class), anyMap());
    }

    @Test
    public void testProvideWhenOnlyPublicSubnetsAndInterfaceServicesWithDifferentAzs() throws IOException, TemplateException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedJson = objectMapper.readTree(new File("src/test/resources/json/aws-cf-network-publicsubnet-vpcendpoints-differentazs.json"));

        when(freeMarkerTemplateUtils.processTemplateIntoString(any(), any())).thenCallRealMethod();
        AmazonEc2Client ec2Client = mock(AmazonEc2Client.class);
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(ec2Client);
        when(ec2Client.describeVpcEndpointServices()).thenReturn(createDescribeVpcEndpointServicesResultWithDifferentAzs());
        NetworkCreationRequest networkCreationRequest = createNetworkRequest(true, PrivateEndpointType.USE_VPC_ENDPOINT);
        List<SubnetRequest> subnetRequestList = createPublicSubnetRequestList();

        String actual = underTest.provide(networkCreationRequest, subnetRequestList);

        JsonNode json = objectMapper.readTree(actual);
        assertEquals(expectedJson, json);
        verify(freeMarkerTemplateUtils).processTemplateIntoString(any(Template.class), anyMap());
    }

    @Test
    public void testProvideWhenPublicSubnetsAndInterfaceServicesAreDisabled() throws IOException, TemplateException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedJson = objectMapper.readTree(new File("src/test/resources/json/aws-cf-network-publicsubnet-onlygatewayvpcendpoints.json"));

        when(freeMarkerTemplateUtils.processTemplateIntoString(any(), any())).thenCallRealMethod();
        NetworkCreationRequest networkCreationRequest = createNetworkRequest(false, PrivateEndpointType.NONE);
        List<SubnetRequest> subnetRequestList = createPrivateAndPublicSubnetRequestList();

        String actual = underTest.provide(networkCreationRequest, subnetRequestList);

        JsonNode json = objectMapper.readTree(actual);
        assertEquals(expectedJson, json);
        verify(freeMarkerTemplateUtils).processTemplateIntoString(any(Template.class), anyMap());
    }

    @Test
    public void testProvideShouldThrowExceptionWhenTemplateProcessHasFailed() throws IOException, TemplateException {
        when(freeMarkerTemplateUtils.processTemplateIntoString(any(Template.class), anyMap())).thenThrow(TemplateException.class);
        NetworkCreationRequest networkCreationRequest = createNetworkRequest(false, PrivateEndpointType.NONE);
        List<SubnetRequest> subnetRequestList = createPublicSubnetRequestList();

        assertThrows(CloudConnectorException.class, () -> underTest.provide(networkCreationRequest, subnetRequestList));

        verify(freeMarkerTemplateUtils).processTemplateIntoString(any(Template.class), anyMap());
    }

    private NetworkCreationRequest createNetworkRequest(boolean privateSubnetEnabled, PrivateEndpointType privateEndpointType) {
        return new NetworkCreationRequest.Builder().withEnvName("envName").withEnvId(1L).withNetworkCidr(VPC_CIDR).withRegion(Region.region("region"))
                .withPrivateSubnetEnabled(privateSubnetEnabled).withEndpointType(privateEndpointType).build();
    }
}
