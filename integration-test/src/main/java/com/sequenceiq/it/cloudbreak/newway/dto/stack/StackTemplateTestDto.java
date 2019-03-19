package com.sequenceiq.it.cloudbreak.newway.dto.stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class StackTemplateTestDto extends StackTestDtoBase<StackTemplateTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackTemplateTestDto.class);

    public StackTemplateTestDto(TestContext testContext) {
        super(testContext);
    }
}
