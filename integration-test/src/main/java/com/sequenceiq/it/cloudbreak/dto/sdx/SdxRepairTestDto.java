package com.sequenceiq.it.cloudbreak.dto.sdx;

import java.util.List;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;

@Prototype
public class SdxRepairTestDto extends AbstractSdxTestDto<SdxRepairRequest, SdxClusterDetailResponse, SdxRepairTestDto> {

    public SdxRepairTestDto(SdxRepairRequest request, TestContext testContext) {
        super(request, testContext);
    }

    public SdxRepairTestDto(TestContext testContext) {
        super(new SdxRepairRequest(), testContext);
    }

    public SdxRepairTestDto() {
        super(SdxRepairTestDto.class.getSimpleName().toUpperCase());
    }

    public SdxRepairTestDto valid() {
        return getCloudProvider().sdxRepair(this);
    }

    public SdxRepairTestDto withHostGroupName(String hostGroupName) {
        getRequest().setHostGroupName(hostGroupName);
        return this;
    }

    public SdxRepairTestDto withHostGroupNames(List<String> hostGroupNames) {
        getRequest().setHostGroupNames(hostGroupNames);
        return this;
    }
}
