package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.common.model.PrivateEndpointType.USE_VPC_ENDPOINT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.ServiceDetail;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsServiceEndpointView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.model.PrivateEndpointType;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Component
public class AwsNetworkCfTemplateProvider {
    public static final String VPC_INTERFACE_SERVICE_ENDPOINT_NAME_PATTERN = "com.amazonaws.%s.%s";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNetworkCfTemplateProvider.class);

    @Inject
    private Configuration freemarkerConfiguration;

    @Value("${cb.aws.cf.network.template.path:}")
    private String cloudFormationNetworkTemplatePath;

    @Value("${cb.aws.vpcendpoints.gateway.services:}")
    private List<String> gatewayServices;

    @Value("${cb.aws.vpcendpoints.interface.services:}")
    private List<String> interfaceServices;

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Inject
    private LegacyAwsClient awsClient;

    public String provide(NetworkCreationRequest networkCreationRequest, List<SubnetRequest> subnets) {

        Map<String, Object> model = createModel(networkCreationRequest, subnets);
        try {
            String freeMarkerTemplate = freemarkerConfiguration.getTemplate(cloudFormationNetworkTemplatePath, "UTF-8").toString();
            Template template = new Template("aws-template", freeMarkerTemplate, freemarkerConfiguration);
            return freeMarkerTemplateUtils.processTemplateIntoString(template, model).replaceAll("\\t|\\n| [\\s]+", "");
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process CloudFormation freemarker template", e);
        }
    }

    private Map<String, Object> createModel(NetworkCreationRequest networkCreationRequest, List<SubnetRequest> subnets) {
        Map<String, Object> model = new HashMap<>();
        model.put("environmentName", networkCreationRequest.getEnvName());
        model.put("environmentId", networkCreationRequest.getEnvId());
        model.put("vpcCidr", networkCreationRequest.getNetworkCidr());
        model.put("subnetDetails", subnets);
        model.put("privateSubnetEnabled", privateSubnetEnabled(networkCreationRequest, subnets));
        model.put("vpcGatewayEndpoints", createGatewayServiceEndpoints());
        model.put("vpcInterfaceEndpoints", createInterfaceServiceEndpointsIfNeeded(networkCreationRequest, subnets));
        return model;
    }

    private List<AwsServiceEndpointView> createGatewayServiceEndpoints() {
        return gatewayServices.stream().map(gs -> new AwsServiceEndpointView(gs, List.of())).collect(Collectors.toList());
    }

    private List<AwsServiceEndpointView> createInterfaceServiceEndpointsIfNeeded(NetworkCreationRequest networkCreationRequest, List<SubnetRequest> subnets) {
        PrivateEndpointType privateEndpointType = networkCreationRequest.getEndpointType();
        if (USE_VPC_ENDPOINT == privateEndpointType && CollectionUtils.isNotEmpty(interfaceServices)) {
            List<AwsServiceEndpointView> interfaceServiceEndpoints = createInterfaceServiceEndpoints(networkCreationRequest, subnets);
            LOGGER.debug("The following interface endpoints will be created in the new vpc: {}", interfaceServiceEndpoints);
            return interfaceServiceEndpoints;
        } else {
            LOGGER.debug("No interface endpoints will be created in the new vpc. serviceEndpointCreation: {}, interfaceServices: {}",
                    privateEndpointType, interfaceServices);
            return List.of();
        }
    }

    private List<AwsServiceEndpointView> createInterfaceServiceEndpoints(NetworkCreationRequest networkCreationRequest, List<SubnetRequest> subnets) {
        Map<String, String> endpointNameMappings = interfaceServices.stream().collect(
                Collectors.toMap(s -> String.format(VPC_INTERFACE_SERVICE_ENDPOINT_NAME_PATTERN, networkCreationRequest.getRegion().value(), s), s -> s));
        List<ServiceDetail> serviceDetails = describeVpcServiceDetails(networkCreationRequest, endpointNameMappings);
        Map<String, SubnetRequest> subnetByZoneMap = createPublicSubnetByZoneMap(subnets);
        List<AwsServiceEndpointView> interfaceServceEndpoints = new ArrayList<>();
        for (ServiceDetail serviceDetail : serviceDetails) {
            List<SubnetRequest> subnetRequests = serviceDetail.getAvailabilityZones().stream()
                    .filter(az -> subnetByZoneMap.containsKey(az)).map(az -> subnetByZoneMap.get(az)).collect(Collectors.toList());
            if (!subnetRequests.isEmpty()) {
                interfaceServceEndpoints.add(new AwsServiceEndpointView(endpointNameMappings.get(serviceDetail.getServiceName()), subnetRequests));
            }
        }
        return interfaceServceEndpoints;
    }

    private Map<String, SubnetRequest> createPublicSubnetByZoneMap(List<SubnetRequest> subnets) {
        return subnets.stream()
                    .filter(s -> StringUtils.isNoneEmpty(s.getPublicSubnetCidr()))
                    .collect(Collectors.toMap(SubnetRequest::getAvailabilityZone, s -> s, (oldValue, newValue) -> oldValue));
    }

    private List<ServiceDetail> describeVpcServiceDetails(NetworkCreationRequest networkCreationRequest, Map<String, String> endpointNameMappings) {
        AwsCredentialView awsCredential = new AwsCredentialView(networkCreationRequest.getCloudCredential());
        AmazonEc2Client awsClientAccess = awsClient.createEc2Client(awsCredential, networkCreationRequest.getRegion().value());
        return awsClientAccess.describeVpcEndpointServices().getServiceDetails().stream()
                .filter(sd -> endpointNameMappings.containsKey(sd.getServiceName())).collect(Collectors.toList());
    }

    private boolean privateSubnetEnabled(NetworkCreationRequest networkRequest, List<SubnetRequest> subnetRequestList) {
        return subnetRequestList.stream().anyMatch(subnetRequest -> !Strings.isNullOrEmpty(subnetRequest.getPrivateSubnetCidr()))
                && networkRequest.isPrivateSubnetEnabled();
    }
}
