package com.sequenceiq.it.cloudbreak.dto.util;

import java.util.List;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.base.RightV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.CheckRightV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class CheckRightTestDto extends AbstractCloudbreakTestDto<Object, CheckRightV4Response, CheckRightTestDto> {

    private List<RightV4> rightsToCheck;

    protected CheckRightTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public CheckRightTestDto valid() {
        return withRightsToCheck(Lists.newArrayList(RightV4.ENV_CREATE));
    }

    public List<RightV4> getRightsToCheck() {
        return rightsToCheck;
    }

    public CheckRightTestDto withRightsToCheck(List<RightV4> rightsToCheck) {
        this.rightsToCheck = rightsToCheck;
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
