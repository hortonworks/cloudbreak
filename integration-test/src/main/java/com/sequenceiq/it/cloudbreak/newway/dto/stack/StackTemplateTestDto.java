package com.sequenceiq.it.cloudbreak.newway.dto.stack;

import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class StackTemplateTestDto extends StackTestDtoBase<StackTemplateTestDto> {

    public StackTemplateTestDto(TestContext testContext) {
        super(testContext);
    }
}
