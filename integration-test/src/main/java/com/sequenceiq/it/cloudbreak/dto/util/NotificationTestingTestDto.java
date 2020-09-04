package com.sequenceiq.it.cloudbreak.dto.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class NotificationTestingTestDto extends AbstractCloudbreakTestDto<Object, Object, NotificationTestingTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationTestingTestDto.class);

    protected NotificationTestingTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public NotificationTestingTestDto valid() {
        return this;
    }

    @Override
    public int order() {
        return 500;
    }

}
