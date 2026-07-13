package com.sequenceiq.it.cloudbreak.dto.remoteenvironment;

import jakarta.inject.Inject;

import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.config.TrustProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractRemoteEnvironmentTestDto;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;

@Prototype
public class DescribeRemoteEnvironmentTestDto extends
        AbstractRemoteEnvironmentTestDto<DescribeRemoteEnvironment, DescribeEnvironmentResponse, DescribeRemoteEnvironmentTestDto> {

    private static final int ORDER = 100;

    @Inject
    private TrustProperties trustProperties;

    public DescribeRemoteEnvironmentTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public DescribeRemoteEnvironmentTestDto valid() {
        String remoteEnvironmentCrn = trustProperties.getRemoteEnvironmentCrn(getTestContext().getActingUserCrn().getAccountId());
        return withCrn(remoteEnvironmentCrn);
    }

    public DescribeRemoteEnvironmentTestDto withCrn(String crn) {
        DescribeRemoteEnvironment request = new DescribeRemoteEnvironment();
        request.setCrn(crn);
        setRequest(request);
        return this;
    }

    @Override
    public int order() {
        return ORDER;
    }
}
