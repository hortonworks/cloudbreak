package com.sequenceiq.it.cloudbreak.dto.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.util.Map;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.sdx.api.model.DatalakeHorizontalScaleRequest;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Prototype
public class SdxScaleTestDto extends AbstractSdxTestDto<DatalakeHorizontalScaleRequest, SdxClusterDetailResponse, SdxScaleTestDto> {

    public SdxScaleTestDto(DatalakeHorizontalScaleRequest request, TestContext testContext) {
        super(request, testContext);
    }

    public SdxScaleTestDto(TestContext testContext) {
        super(new DatalakeHorizontalScaleRequest(), testContext);
    }

    public SdxScaleTestDto await(SdxClusterStatusResponse status) {
        return getTestContext().await(this, Map.of("status", status), emptyRunningParameter());
    }

    @Override
    public SdxScaleTestDto valid() {
        return this;
    }

    public SdxScaleTestDto withName(String dataLakeName) {
        setName(dataLakeName);
        return this;
    }

    public SdxScaleTestDto withGroup(String instanceGroupName) {
        getRequest().setGroup(instanceGroupName);
        return this;
    }

    public SdxScaleTestDto withDesiredCount(int desiredCount) {
        getRequest().setDesiredCount(desiredCount);
        return this;
    }

    @Override
    public String getCrn() {
        if (getResponse() == null) {
            throw new IllegalStateException("You have tried to assign to SdxScaleTestDto," +
                    " that hasn't been created and therefore has no Response object yet.");
        }
        return getResponse().getCrn();
    }
}
