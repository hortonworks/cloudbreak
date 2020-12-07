package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.SparkUri;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.DefaultResponseConfigure;
import com.sequenceiq.it.cloudbreak.mock.ExecuteQueryToMockInfrastructure;

public class FreeIPAEndpoints<T extends CloudbreakTestDto> {

    private T testDto;

    private ExecuteQueryToMockInfrastructure executeQueryToMockInfrastructure;

    public FreeIPAEndpoints(T testDto, ExecuteQueryToMockInfrastructure executeQueryToMockInfrastructure) {
        this.testDto = testDto;
        this.executeQueryToMockInfrastructure = executeQueryToMockInfrastructure;
    }

    public Session<T> session() {
        return (Session<T>) EndpointProxyFactory.create(Session.class, testDto, executeQueryToMockInfrastructure);
    }

    @SparkUri(url = "/{mockUuid}/ipa/session/json")
    public interface Session<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> post();
    }
}
