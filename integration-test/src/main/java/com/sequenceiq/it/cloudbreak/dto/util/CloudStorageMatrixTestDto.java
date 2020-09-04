package com.sequenceiq.it.cloudbreak.dto.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.CloudStorageSupportedV4Response;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class CloudStorageMatrixTestDto extends AbstractCloudbreakTestDto<Object, CloudStorageSupportedV4Response, CloudStorageMatrixTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageMatrixTestDto.class);

    private String stackVersion;

    protected CloudStorageMatrixTestDto(TestContext testContext) {
        super(null, testContext);
    }

    public String getStackVersion() {
        return stackVersion;
    }

    public CloudStorageMatrixTestDto withStackVersion(String stackVersion) {
        this.stackVersion = stackVersion;
        return this;
    }

    @Override
    public CloudStorageMatrixTestDto valid() {
        return withStackVersion("3.1");
    }

    @Override
    public int order() {
        return 500;
    }

}
