package com.sequenceiq.it.cloudbreak.dto.externalizedcompute;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.util.Map;

import com.sequenceiq.environment.api.v1.environment.model.request.ExternalizedComputeCreateRequest;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterApiStatus;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.microservice.ExternalizedComputeClusterClient;

@Prototype
public class ExternalizedComputeClusterTestDto extends AbstractTestDto<ExternalizedComputeCreateRequest, ExternalizedComputeClusterResponse,
        ExternalizedComputeClusterTestDto, ExternalizedComputeClusterClient> {

    private String environmentKey;

    protected ExternalizedComputeClusterTestDto(TestContext testContext) {
        super(null, testContext);
    }

    public String getEnvironmentCrn() {
        return environmentKey == null ? getTestContext().get(EnvironmentTestDto.class).getCrn() : getTestContext().get(environmentKey).getCrn();
    }

    public ExternalizedComputeClusterTestDto withEnvironment(String environmentKey) {
        this.environmentKey = environmentKey;
        return this;
    }

    @Override
    public ExternalizedComputeClusterTestDto valid() {
        return getCloudProvider().externalizedComputeCluster(this);
    }

    public ExternalizedComputeClusterTestDto await(ExternalizedComputeClusterApiStatus status) {
        return getTestContext().await(this, Map.of("status", status), emptyRunningParameter());
    }

    @Override
    public ExternalizedComputeClusterTestDto when(Action<ExternalizedComputeClusterTestDto, ExternalizedComputeClusterClient> action) {
        return getTestContext().when(this, ExternalizedComputeClusterClient.class, action, emptyRunningParameter());
    }

    @Override
    public String getCrn() {
        // TODO: Use externalized compute cluster crn if it will be exposed through the API
        return getEnvironmentCrn();
    }
}
