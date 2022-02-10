package com.sequenceiq.it.cloudbreak.dto.util;

import java.util.List;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.info.model.CheckRightV4Response;
import com.sequenceiq.authorization.info.model.RightV4;
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
    public int order() {
        return 500;
    }

}
