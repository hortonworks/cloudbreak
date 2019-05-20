package com.sequenceiq.it.cloudbreak.dto.stack;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;

@Prototype
public class StackTemplateTestDto extends StackTestDtoBase<StackTemplateTestDto> {

    public StackTemplateTestDto(TestContext testContext) {
        super(testContext);
    }
}
