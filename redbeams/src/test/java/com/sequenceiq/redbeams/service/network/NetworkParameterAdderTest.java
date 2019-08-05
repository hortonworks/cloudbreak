package com.sequenceiq.redbeams.service.network;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.collection.IsMapContaining;
import org.junit.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams.EnvironmentNetworkAwsParamsBuilder;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse.EnvironmentNetworkResponseBuilder;
import com.sequenceiq.environment.api.v1.environment.model.response.SecurityAccessResponse;

public class NetworkParameterAdderTest {

    private static final String VPC_SECURITY_CIDR = "1.2.3.4/16";

    private static final String VPC_ID = "vpcId";

    private final NetworkParameterAdder underTest = new NetworkParameterAdder();

    @Test
    public void testAddNetworkParameters() {
        Map<String, Object> parameters = new HashMap<>();
        List<String> subnetIds = List.of("subnet1", "subnet2");

        parameters = underTest.addNetworkParameters(parameters, subnetIds);

        assertThat(parameters, IsMapContaining.hasEntry("subnetId", String.join(",", subnetIds)));
    }

    @Test
    public void testAddVpcParametersWhenAws() {
        Map<String, Object> parameters = new HashMap<>();
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder.builder()
                .withCloudPlatform(CloudPlatform.AWS.name())
                .withSecurityAccess(SecurityAccessResponse.builder()
                        .withCidr(VPC_SECURITY_CIDR).build())
                .withNetwork(EnvironmentNetworkResponseBuilder.anEnvironmentNetworkResponse()
                        .withAws(EnvironmentNetworkAwsParamsBuilder.anEnvironmentNetworkAwsParams().withVpcId(VPC_ID).build())
                        .build())
                .build();

        parameters = underTest.addVpcParameters(parameters, environment, CloudPlatform.AWS);

        assertThat(parameters, IsMapContaining.hasEntry("vpcId", VPC_ID));
        assertThat(parameters, IsMapContaining.hasEntry("vpcCidr", VPC_SECURITY_CIDR));
    }
}
