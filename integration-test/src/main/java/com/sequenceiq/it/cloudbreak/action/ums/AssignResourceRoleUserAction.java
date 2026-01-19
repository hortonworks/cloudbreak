package com.sequenceiq.it.cloudbreak.action.ums;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.UmsClient;

public class AssignResourceRoleUserAction extends AbstractUmsAction<UmsTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignResourceRoleUserAction.class);

    private final String userKey;

    public AssignResourceRoleUserAction(String userKey) {
        this.userKey = userKey;
    }

    @Override
    protected UmsTestDto umsAction(TestContext testContext, UmsTestDto testDto, UmsClient client) throws Exception {
        CloudbreakUser user = testContext.getTestUsers().getUserByLabel(userKey);
        String userCrn = user.getCrn();
        String resourceCrn = testDto.getRequest().getResourceCrn();
        String resourceRole = UmsClientUtils.getResourceRoleCrn(testDto, client);

        Log.when(LOGGER, format(" Assigning resource role '%s' to user '%s' at resource '%s'... ", resourceRole, userCrn, resourceCrn));
        Log.whenJson(LOGGER, format(" Assign resource role request:%n "), testDto.getRequest());
        LOGGER.info(format(" Assigning resource role '%s' to user '%s' at resource '%s'... ", resourceRole, userCrn, resourceCrn));
        Multimap<String, String> assignedResourceRoles = client.getDefaultClient(testContext).listAssignedResourceRoles(userCrn);
        if (assignedResourceRoles.get(resourceCrn).contains(resourceRole)) {
            LOGGER.info(format(" Resource role '%s' has already been assigned to user '%s' at resource '%s' ", resourceRole, userCrn, resourceCrn));
            Log.when(LOGGER, format(" Resource role '%s' has already been assigned to user '%s' at resource '%s' ", resourceRole, userCrn, resourceCrn));
        } else {
            client.getDefaultClient(testContext).assignResourceRole(userCrn, resourceCrn, resourceRole);
            // wait for UmsRightsCache to expire
            Thread.sleep(7000);
            LOGGER.info(format(" Resource role '%s' has been assigned to user '%s' at resource '%s' ", resourceRole, userCrn, resourceCrn));
            Log.when(LOGGER, format(" Resource role '%s' has been assigned to user '%s' at resource '%s' ", resourceRole, userCrn, resourceCrn));
        }
        return testDto;
    }
}
