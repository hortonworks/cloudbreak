package com.sequenceiq.it.cloudbreak.action.ums;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.UmsClient;

public class UnassignResourceRoleGroupAction extends AbstractUmsAction<UmsTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnassignResourceRoleGroupAction.class);

    private final String groupCrn;

    public UnassignResourceRoleGroupAction(String groupCrn) {
        this.groupCrn = groupCrn;
    }

    @Override
    protected UmsTestDto umsAction(TestContext testContext, UmsTestDto testDto, UmsClient client) throws Exception {
        String resourceCrn = testDto.getRequest().getResourceCrn();
        String resourceRole = UmsClientUtils.getResourceRoleCrn(testDto, client);

        Log.when(LOGGER, format(" Revoke resource role '%s' from group '%s' at resource '%s' ", resourceRole, groupCrn, resourceCrn));
        Log.whenJson(LOGGER, format(" Revoke resource role request:%n "), testDto.getRequest());
        LOGGER.info(format(" Revoking resource role '%s' from group '%s' at resource '%s'... ", resourceRole, groupCrn, resourceCrn));
        Multimap<String, String> assignedResourceRoles = client.getDefaultClient(testContext).listAssignedResourceRoles(groupCrn);
        if (assignedResourceRoles.get(resourceCrn).contains(resourceRole)) {
            client.getDefaultClient(testContext).unassignResourceRole(groupCrn, resourceCrn, resourceRole);
            // wait for UmsRightsCache to expire
            Thread.sleep(7000);
            LOGGER.info(format(" Resource role '%s' has been revoked at resource '%s' from group '%s' ", resourceRole, resourceCrn, groupCrn));
            Log.when(LOGGER, format(" Resource role '%s' has been revoked at resource '%s' from group '%s' ", resourceRole, resourceCrn, groupCrn));
        } else {
            LOGGER.info(format(" Resource role '%s' has already been revoked from group '%s' at resource '%s' ", resourceRole, groupCrn, resourceCrn));
            Log.when(LOGGER, format(" Resource role '%s' has already been revoked from group '%s' at resource '%s' ", resourceRole, groupCrn, resourceCrn));
        }
        return testDto;
    }
}
