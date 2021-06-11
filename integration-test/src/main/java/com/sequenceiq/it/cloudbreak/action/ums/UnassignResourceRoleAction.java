package com.sequenceiq.it.cloudbreak.action.ums;

import static java.lang.String.format;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class UnassignResourceRoleAction extends AbstractUmsAction<UmsTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnassignResourceRoleAction.class);

    private final String userKey;

    public UnassignResourceRoleAction(String userKey) {
        this.userKey = userKey;
    }

    @Override
    protected UmsTestDto umsAction(TestContext testContext, UmsTestDto testDto, UmsClient client) throws Exception {
        CloudbreakUser user = testContext.getRealUmsUserByKey(userKey);
        String resourceRole = testDto.getRequest().getRoleCrn();
        String resourceCrn = testDto.getRequest().getResourceCrn();
        Log.when(LOGGER, format(" Remove resource role '%s' at resource '%s' for user '%s' ", resourceRole, resourceCrn, user.getCrn()));
        Log.whenJson(LOGGER, format(" Remove resource role request:%n "), testDto.getRequest());
        client.getDefaultClient().unassignResourceRole(user.getCrn(), resourceCrn, resourceRole, Optional.of(""));
        // wait for UmsRightsCache to expire
        Thread.sleep(7000);
        LOGGER.info(format(" Resource role '%s' has been removed at resource '%s' for user '%s' ", resourceRole, resourceCrn, user.getCrn()));
        Log.when(LOGGER, format(" Resource role '%s' has been removed at resource '%s' for user '%s' ", resourceRole, resourceCrn, user.getCrn()));
        return testDto;
    }
}
