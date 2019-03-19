package com.sequenceiq.it.cloudbreak.newway.dto.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.VersionCheckV4Result;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.AbstractCloudbreakTestDto;

@Prototype
public class VersionCheckTestDto extends AbstractCloudbreakTestDto<Object, VersionCheckV4Result, VersionCheckTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionCheckTestDto.class);

    private String version;

    protected VersionCheckTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public VersionCheckTestDto valid() {
        return withVersion("3.1");
    }

    public String getVersion() {
        return version;
    }

    public VersionCheckTestDto withVersion(String version) {
        this.version = version;
        return this;
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.debug("this entry point does not have any clean up operation");
    }

    @Override
    public int order() {
        return 500;
    }

}
