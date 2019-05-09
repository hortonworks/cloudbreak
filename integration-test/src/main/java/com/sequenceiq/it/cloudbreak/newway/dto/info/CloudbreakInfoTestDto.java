package com.sequenceiq.it.cloudbreak.newway.dto.info;

import com.sequenceiq.cloudbreak.api.endpoint.v4.info.responses.CloudbreakInfoResponse;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.AbstractCloudbreakTestDto;

@Prototype
public class CloudbreakInfoTestDto extends AbstractCloudbreakTestDto<InfoRequest, CloudbreakInfoResponse, CloudbreakInfoTestDto> {

    public static final String INFO = "INFO";

    CloudbreakInfoTestDto(String newId) {
        super(newId);
        setRequest(new InfoRequest());
    }

    CloudbreakInfoTestDto() {
        this(INFO);
    }

    public CloudbreakInfoTestDto(TestContext testContext) {
        super(new InfoRequest(), testContext);
    }

    public CloudbreakInfoTestDto valid() {
        return this;
    }
}
