package com.sequenceiq.it.cloudbreak.newway.dto.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.RepoConfigValidationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.RepoConfigValidationV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.AbstractCloudbreakTestDto;

@Prototype
public class RepoConfigValidationTestDto extends AbstractCloudbreakTestDto<RepoConfigValidationV4Request, RepoConfigValidationV4Response,
        RepoConfigValidationTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepoConfigValidationTestDto.class);

    protected RepoConfigValidationTestDto(TestContext testContext) {
        super(new RepoConfigValidationV4Request(), testContext);
    }

    private RepoConfigValidationTestDto withAmbariBaseUrl(String ambariBaseUrl) {
        getRequest().setAmbariBaseUrl(ambariBaseUrl);
        return this;
    }

    public RepoConfigValidationTestDto withAmbariGpgKeyUrl(String ambariGpgKeyUrl) {
        getRequest().setAmbariGpgKeyUrl(ambariGpgKeyUrl);
        return this;
    }

    public RepoConfigValidationTestDto withStackBaseURL(String stackBaseURL) {
        getRequest().setStackBaseURL(stackBaseURL);
        return this;
    }

    public RepoConfigValidationTestDto withUtilsBaseURL(String utilsBaseURL) {
        getRequest().setUtilsBaseURL(utilsBaseURL);
        return this;
    }

    public RepoConfigValidationTestDto withVersionDefinitionFileUrl(String versionDefinitionFileUrl) {
        getRequest().setVersionDefinitionFileUrl(versionDefinitionFileUrl);
        return this;
    }

    public RepoConfigValidationTestDto withMpackUrl(String mpackUrl) {
        getRequest().setMpackUrl(mpackUrl);
        return this;
    }

    public RepoConfigValidationTestDto withRequest(RepoConfigValidationV4Request request) {
        setRequest(request);
        return this;
    }

    @Override
    public RepoConfigValidationTestDto valid() {
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
