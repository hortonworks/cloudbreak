package com.sequenceiq.it.cloudbreak.action.ums;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;

public class AssignUmsRoleAction implements Action<UmsTestDto, UmsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignUmsRoleAction.class);

    private final String userKey;

    public AssignUmsRoleAction(String userKey) {
        this.userKey = userKey;
    }

    @Override
    public UmsTestDto action(TestContext testContext, UmsTestDto testDto, UmsClient client) throws Exception {
        CloudbreakUser user = testContext.getRealUmsUserByKey(userKey);
        LOGGER.info(String.format("Assigning resourceRole %s over resource %s for user ",
                testDto.getRequest().getRoleCrn(), testDto.getRequest().getResourceCrn()), user.getCrn());
        client.getDefaultClient().assignResourceRole(user.getCrn(), testDto.getRequest().getResourceCrn(), testDto.getRequest().getRoleCrn(), Optional.of(""));
        // wait for UmsRightsCache to expire
        Thread.sleep(7000);
        return testDto;
    }
}
