package com.sequenceiq.it.cloudbreak.dto.util;

import java.util.List;
import java.util.Map;

import com.sequenceiq.authorization.info.model.CheckResourceRightsV4Response;
import com.sequenceiq.authorization.info.model.RightV4;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class CheckResourceRightTestDto extends AbstractCloudbreakTestDto<Object, CheckResourceRightsV4Response, CheckResourceRightTestDto> {

    private Map<String, List<RightV4>> rightsToCheck;

    protected CheckResourceRightTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public CheckResourceRightTestDto valid() {
        return this;
    }

    public Map<String, List<RightV4>> getRightsToCheck() {
        return rightsToCheck;
    }

    public CheckResourceRightTestDto withRightsToCheck(Map<String, List<RightV4>> rightsToCheck) {
        this.rightsToCheck = rightsToCheck;
        return this;
    }

    @Override
    public int order() {
        return 500;
    }

}
