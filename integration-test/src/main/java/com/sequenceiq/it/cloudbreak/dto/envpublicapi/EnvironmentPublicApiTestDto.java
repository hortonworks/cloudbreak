package com.sequenceiq.it.cloudbreak.dto.envpublicapi;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentRequest;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentPublicApiClient;

@Prototype
public class EnvironmentPublicApiTestDto extends AbstractEnvironmentPublicApiTestDto<DescribeEnvironmentRequest, DescribeEnvironmentResponse,
        EnvironmentPublicApiTestDto> {
    public EnvironmentPublicApiTestDto(TestContext testContext) {
        super(new DescribeEnvironmentRequest(), testContext);
    }

    @Override
    public EnvironmentPublicApiTestDto valid() {
        return this;
    }

    public EnvironmentPublicApiTestDto withNameOrCrn(String nameOrCrn) {
        getRequest().setEnvironmentName(nameOrCrn);
        return this;
    }

    public EnvironmentPublicApiTestDto when(Action<EnvironmentPublicApiTestDto, EnvironmentPublicApiClient> action) {
        return getTestContext().when(this, EnvironmentPublicApiClient.class, action, emptyRunningParameter());
    }
}
