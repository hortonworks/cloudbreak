package com.sequenceiq.it.cloudbreak.dto;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;

@Prototype
public class RawCloudbreakTestDto extends AbstractCloudbreakTestDto<String, String, RawCloudbreakTestDto> {

    private String requestJson;

    protected RawCloudbreakTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public CloudbreakTestDto valid() {
        return withRequest("{}");
    }

    public RawCloudbreakTestDto withRequest(String requestJson) {
        this.requestJson = requestJson;
        return this;
    }

    public String getRequestJson() {
        return requestJson;
    }
}
