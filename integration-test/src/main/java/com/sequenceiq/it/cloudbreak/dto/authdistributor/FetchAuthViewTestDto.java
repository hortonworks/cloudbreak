package com.sequenceiq.it.cloudbreak.dto.authdistributor;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UserState;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.microservice.AuthDistributorClient;
import com.sequenceiq.it.cloudbreak.request.authdistributor.FetchAuthViewRequest;

@Prototype
public class FetchAuthViewTestDto extends AbstractTestDto<FetchAuthViewRequest, UserState, FetchAuthViewTestDto, AuthDistributorClient> {

    public FetchAuthViewTestDto(TestContext testContext) {
        super(new FetchAuthViewRequest(), testContext);
    }

    public FetchAuthViewTestDto withEnvironmentCrn(String environmentCrn) {
        getRequest().setEnvironmentCrn(environmentCrn);
        return this;
    }

    @Override
    public FetchAuthViewTestDto then(Assertion<FetchAuthViewTestDto, AuthDistributorClient> assertion) {
        return then(assertion, emptyRunningParameter());
    }

    @Override
    public FetchAuthViewTestDto then(Assertion<FetchAuthViewTestDto, AuthDistributorClient> assertion, RunningParameter runningParameter) {
        return getTestContext().then(this, AuthDistributorClient.class, assertion, runningParameter);
    }

    public FetchAuthViewTestDto valid() {
        return this;
    }

}
