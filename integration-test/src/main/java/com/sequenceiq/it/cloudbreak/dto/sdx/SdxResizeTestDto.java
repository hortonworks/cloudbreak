package com.sequenceiq.it.cloudbreak.dto.sdx;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.sdx.api.model.SdxClusterResizeRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Prototype
public class SdxResizeTestDto extends AbstractSdxTestDto<SdxClusterResizeRequest, SdxClusterResponse, SdxResizeTestDto> {

    public SdxResizeTestDto(SdxClusterResizeRequest request, TestContext testContext) {
        super(request, testContext);
    }

    public SdxResizeTestDto(TestContext testContext) {
        super(new SdxClusterResizeRequest(), testContext);
    }

    public SdxResizeTestDto() {
        super(SdxResizeTestDto.class.getSimpleName().toUpperCase());
    }

    @Override
    public SdxResizeTestDto valid() {
        return withEnvironmentName(getTestContext().get(EnvironmentTestDto.class).getResponse().getName())
                .withClusterShape(getCloudProvider().getClusterShape());
    }

    public SdxResizeTestDto withEnvironmentName(String environment) {
        getRequest().setEnvironment(environment);
        return this;
    }

    public SdxResizeTestDto withClusterShape(SdxClusterShape clusterShape) {
        getRequest().setClusterShape(clusterShape);
        return this;
    }
}
