package com.sequenceiq.it.cloudbreak.dto.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.DeploymentPreferencesV4Response;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class DeploymentPreferencesTestDto extends AbstractCloudbreakTestDto<Object, DeploymentPreferencesV4Response, DeploymentPreferencesTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentPreferencesTestDto.class);

    protected DeploymentPreferencesTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public DeploymentPreferencesTestDto valid() {
        return this;
    }

    @Override
    public int order() {
        return 500;
    }

}
