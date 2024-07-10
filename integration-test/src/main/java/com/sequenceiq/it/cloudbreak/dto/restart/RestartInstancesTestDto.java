package com.sequenceiq.it.cloudbreak.dto.restart;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class RestartInstancesTestDto extends AbstractCloudbreakTestDto<Void, FlowIdentifier, RestartInstancesTestDto> {

    private String crn;

    public RestartInstancesTestDto(TestContext testContext) {
        super(null, testContext);
    }

    public RestartInstancesTestDto withName(String name) {
        setName(name);
        return this;
    }

    public RestartInstancesTestDto withCrn(String crn) {
        this.crn = crn;
        return this;
    }

    @Override
    public String getCrn() {
        return crn;
    }

    @Override
    public RestartInstancesTestDto valid() {
        return this;
    }
}
