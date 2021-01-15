package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.SparkUri;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.DefaultResponseConfigure;

public class FreeIPAEndpoints<T extends CloudbreakTestDto> {

    private T testDto;

    private MockedTestContext mockedTestContext;

    public FreeIPAEndpoints(T testDto, MockedTestContext mockedTestContext) {
        this.testDto = testDto;
        this.mockedTestContext = mockedTestContext;
    }

    public Session<T> session() {
        return (Session<T>) EndpointProxyFactory.create(Session.class, testDto, mockedTestContext);
    }

    @SparkUri(url = "/{mockUuid}/ipa/session/json")
    public interface Session<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T, Object> post();
    }
}
