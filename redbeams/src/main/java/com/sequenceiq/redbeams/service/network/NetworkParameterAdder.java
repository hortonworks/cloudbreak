package com.sequenceiq.redbeams.service.network;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.converter.spi.ServiceEndpointCreationToEndpointTypeConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.exception.RedbeamsException;

@Service
public class NetworkParameterAdder {

    // These constants must match those in AwsNetworkView

    @VisibleForTesting
    static final String VPC_ID = "vpcId";

    @VisibleForTesting
    static final String VPC_CIDR = "vpcCidr";

    @VisibleForTesting
    static final String VPC_CIDRS = "vpcCidrs";

    @VisibleForTesting
    static final String SUBNET_ID = "subnetId";

    @VisibleForTesting
    static final String ENDPOINT_TYPE = "endpointType";

    @VisibleForTesting
    static final String SUBNET_FOR_PRIVATE_ENDPOINT = "subnetForPrivateEndpoint";

    @VisibleForTesting
    static final String AVAILABILITY_ZONE = "availabilityZone";

    // These constants must match those in AzureNetworkView
    @VisibleForTesting
    static final String SUBNETS = "subnets";

    @Inject
    private ServiceEndpointCreationToEndpointTypeConverter serviceEndpointCreationToEndpointTypeConverter;

    @Inject
    private SubnetListerService subnetListerService;

    @Inject
    private SubnetChooserService subnetChooserService;

    public Map<String, Object> addSubnetIds(Map<String, Object> parameters, List<String> subnetIds, List<String> azs, CloudPlatform cloudPlatform) {
        switch (cloudPlatform) {
            case AWS:
            case GCP:
            case MOCK:
                parameters.put(SUBNET_ID, String.join(",", subnetIds));
                parameters.put(AVAILABILITY_ZONE, String.join(",", azs));
                break;
            case AZURE:
                parameters.put(SUBNETS, String.join(",", subnetIds));
                break;
            default:
                throw new RedbeamsException(String.format("Support for cloud platform %s not yet added", cloudPlatform.name()));
        }
        return parameters;
    }

    public Map<String, Object> addParameters(
            Map<String, Object> parameters, DetailedEnvironmentResponse environmentResponse, CloudPlatform cloudPlatform, DBStack dbStack) {
        switch (cloudPlatform) {
            case AWS:
                parameters.put(VPC_CIDR, environmentResponse.getNetwork().getNetworkCidr());
                parameters.put(VPC_CIDRS, environmentResponse.getNetwork().getNetworkCidrs());
                parameters.put(VPC_ID, environmentResponse.getNetwork().getAws().getVpcId());
                break;
            case AZURE:
                parameters.put(
                        ENDPOINT_TYPE, serviceEndpointCreationToEndpointTypeConverter.convert(environmentResponse.getNetwork().getServiceEndpointCreation()));
                parameters.put(SUBNET_FOR_PRIVATE_ENDPOINT, getAzureSubnetToUseWithPrivateEndpoint(environmentResponse, dbStack));
                break;
            case GCP:
                // oddly, nothing to pass on yet
                break;
            case MOCK:
                parameters.put(VPC_ID, environmentResponse.getNetwork().getMock().getVpcId());
                break;
            default:
                throw new RedbeamsException(String.format("Support for cloud platform %s not yet added", cloudPlatform.name()));
        }
        return parameters;
    }

    private String getAzureSubnetToUseWithPrivateEndpoint(DetailedEnvironmentResponse detailedEnvironmentResponse, DBStack dbStack) {
        String subscriptionId = subnetListerService.getAzureSubscriptionId(detailedEnvironmentResponse.getCrn());
        return subnetChooserService.chooseSubnetForPrivateEndpoint(detailedEnvironmentResponse.getNetwork().getSubnetMetas().values(), dbStack).stream()
                .findFirst()
                .map(csn -> SubnetListerService.expandAzureResourceId(csn, detailedEnvironmentResponse, subscriptionId))
                .map(sn -> sn.getId()).orElse(null);
    }
}
