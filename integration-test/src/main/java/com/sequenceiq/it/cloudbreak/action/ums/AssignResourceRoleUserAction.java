package com.sequenceiq.it.cloudbreak.action.ums;

import static java.lang.String.format;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class AssignResourceRoleUserAction extends AbstractUmsAction<UmsTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignResourceRoleUserAction.class);

    private final String userKey;

    public AssignResourceRoleUserAction(String userKey) {
        this.userKey = userKey;
    }

    @Override
    protected UmsTestDto umsAction(TestContext testContext, UmsTestDto testDto, UmsClient client) throws Exception {
        CloudbreakUser user = testContext.getRealUmsUserByKey(userKey);
        String resourceRole = testDto.getRequest().getRoleCrn();
        String resourceCrn = testDto.getRequest().getResourceCrn();
        Log.when(LOGGER, format(" Assigning resource role '%s' at resource '%s' for user '%s' ", resourceRole, resourceCrn, user.getCrn()));
        Log.whenJson(LOGGER, format(" Assign resource role request:%n "), testDto.getRequest());

        Multimap<String, String> assignedResourceRoles = client.getDefaultClient().listAssignedResourceRoles(user.getCrn(), Optional.of(""));
        LOGGER.info(format(" Assigned resource roles ['%s'] are present to user '%s' ", assignedResourceRoles, user.getCrn()));
        if (assignedResourceRoles.get(resourceCrn).contains(resourceRole)) {
            LOGGER.info(format(" Resource role '%s' has already been assigned to user '%s' for resource '%s' ", resourceRole, user.getCrn(), resourceCrn));
            Log.when(LOGGER, format(" Resource role '%s' has already been assigned to user '%s' for resource '%s' ", resourceRole, user.getCrn(), resourceCrn));
        } else {
            client.getDefaultClient().assignResourceRole(user.getCrn(), resourceCrn, resourceRole, Optional.of(""));
            // wait for UmsRightsCache to expire
            Thread.sleep(7000);
            LOGGER.info(format(" Resource role '%s' has been assigned at resource '%s' for user '%s' ", resourceRole, resourceCrn, user.getCrn()));
            Log.when(LOGGER, format(" Resource role '%s' has been assigned at resource '%s' for user '%s' ", resourceRole, resourceCrn, user.getCrn()));
        }
        return testDto;
    }
}
