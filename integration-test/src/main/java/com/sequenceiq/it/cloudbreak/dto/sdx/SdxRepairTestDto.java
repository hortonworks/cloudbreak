package com.sequenceiq.it.cloudbreak.dto.sdx;

import java.util.UUID;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;

@Prototype
public class SdxRepairTestDto extends AbstractSdxTestDto<SdxRepairRequest, SdxClusterDetailResponse, SdxRepairTestDto> {

    private static final String DEFAULT_SDX_NAME = "test-sdx" + '-' + UUID.randomUUID().toString().replaceAll("-", "");

    private static final String HOSTGROUP_NAME = HostGroupType.IDBROKER.getName();

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
        withSdxName(getResourcePropertyProvider().getName())
                .withHostGroupName(HOSTGROUP_NAME);
        return getCloudProvider().sdxRepair(this);
    }

    public SdxRepairTestDto withHostGroupName() {
        return withHostGroupName(HOSTGROUP_NAME);
    }

    public SdxRepairTestDto withHostGroupName(String hostGroupName) {
        getRequest().setHostGroupName(hostGroupName);
        return this;
    }

    public String getHostGroupName() {
        return getRequest().getHostGroupName();
    }

    public SdxRepairTestDto withSdxName(String name) {
        setName(name);
        return this;
    }

    @Override
    public String getName() {
        return super.getName() == null ? DEFAULT_SDX_NAME : super.getName();
    }
}
