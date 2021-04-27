package com.sequenceiq.it.cloudbreak.action.ums;

import static java.lang.String.format;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class AssignResourceRoleUserAction implements Action<UmsTestDto, UmsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignResourceRoleUserAction.class);

    private final String userKey;

    public AssignResourceRoleUserAction(String userKey) {
        this.userKey = userKey;
    }

    @Override
    public UmsTestDto action(TestContext testContext, UmsTestDto testDto, UmsClient client) throws Exception {
        CloudbreakUser user = testContext.getRealUmsUserByKey(userKey);
        String resourceRole = testDto.getRequest().getRoleCrn();
        String resourceCrn = testDto.getRequest().getResourceCrn();
        Log.when(LOGGER, format(" Assigning resource role '%s' at resource '%s' for user '%s' ", resourceRole, resourceCrn, user.getCrn()));
        Log.whenJson(LOGGER, format(" Assign resource role request:%n "), testDto.getRequest());
        client.getDefaultClient().assignResourceRole(user.getCrn(), resourceCrn, resourceRole, Optional.of(""));
        // wait for UmsRightsCache to expire
        Thread.sleep(7000);
        LOGGER.info(format(" Resource role '%s' has been assigned at resource '%s' for user '%s' ", resourceRole, resourceCrn, user.getCrn()));
        Log.when(LOGGER, format(" Resource role '%s' has been assigned at resource '%s' for user '%s' ", resourceRole, resourceCrn, user.getCrn()));
        return testDto;
    }
}
