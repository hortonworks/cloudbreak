package com.sequenceiq.it.cloudbreak.newway.entity.deploymentpref;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.DeploymentPreferencesV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AbstractCloudbreakEntity;

@Prototype
public class DeploymentPreferencesTestDto extends AbstractCloudbreakEntity<Object, DeploymentPreferencesV4Response, DeploymentPreferencesTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentPreferencesTestDto.class);

    protected DeploymentPreferencesTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public DeploymentPreferencesTestDto valid() {
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
