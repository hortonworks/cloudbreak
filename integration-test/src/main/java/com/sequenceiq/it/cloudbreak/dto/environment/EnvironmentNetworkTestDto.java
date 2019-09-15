package com.sequenceiq.it.cloudbreak.dto.environment;

import java.util.Set;

import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkYarnParams;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class EnvironmentNetworkTestDto extends AbstractCloudbreakTestDto<EnvironmentNetworkRequest, EnvironmentNetworkResponse, EnvironmentNetworkTestDto> {
    public static final String ENV_NETWORK = "ENV_NETWORK";

    public EnvironmentNetworkTestDto(EnvironmentNetworkRequest request, TestContext testContext) {
        super(request, testContext);
    }

    public EnvironmentNetworkTestDto(TestContext testContext) {
        super(new EnvironmentNetworkRequest(), testContext);
    }

    public EnvironmentNetworkTestDto() {
        super(EnvironmentNetworkTestDto.class.getSimpleName().toUpperCase());
    }

    public EnvironmentNetworkTestDto valid() {
        return getCloudProvider().network(this);
    }

    public EnvironmentNetworkTestDto withAzure(EnvironmentNetworkAzureParams azure) {
        getRequest().setAzure(azure);
        return this;
    }

    public EnvironmentNetworkTestDto withAws(EnvironmentNetworkAwsParams aws) {
        getRequest().setAws(aws);
        return this;
    }

    public EnvironmentNetworkTestDto withYarn(EnvironmentNetworkYarnParams yarn) {
        getRequest().setYarn(yarn);
        return this;
    }

    public EnvironmentNetworkTestDto withNetworkCIDR(String networkCIDR) {
        getRequest().setNetworkCidr(networkCIDR);
        return this;
    }

    public EnvironmentNetworkTestDto withSubnetIDs(Set<String> subnetIDs) {
        getRequest().setSubnetIds(subnetIDs);
        return this;
    }

    public EnvironmentNetworkTestDto withMock(EnvironmentNetworkMockParams mock) {
        getRequest().setMock(mock);
        return this;
    }

}
