package com.sequenceiq.it.cloudbreak.dto.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class StackMatrixTestDto extends AbstractCloudbreakTestDto<Object, StackMatrixV4Response, StackMatrixTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackMatrixTestDto.class);

    private boolean govCloud;

    private String os;

    protected StackMatrixTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public StackMatrixTestDto valid() {
        return this;
    }

    public StackMatrixTestDto withGovCloud(boolean govCloud) {
        this.govCloud = govCloud;
        return this;
    }

    public StackMatrixTestDto withOs(String os) {
        this.os = os;
        return this;
    }

    public boolean isGovCloud() {
        return govCloud;
    }

    public String getOs() {
        return os;
    }

    @Override
    public int order() {
        return 500;
    }

}
