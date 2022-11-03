package com.sequenceiq.it.cloudbreak.action.ums;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class UnassignResourceRoleAction extends AbstractUmsAction<UmsTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnassignResourceRoleAction.class);

    private final String userKey;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public UnassignResourceRoleAction(String userKey, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.userKey = userKey;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    @Override
    protected UmsTestDto umsAction(TestContext testContext, UmsTestDto testDto, UmsClient client) throws Exception {
        CloudbreakUser user = testContext.getRealUmsUserByKey(userKey);
        String userCrn = user.getCrn();
        String resourceRole = testDto.getRequest().getRoleCrn();
        String resourceCrn = testDto.getRequest().getResourceCrn();

        Log.when(LOGGER, format(" Revoke resource role '%s' from user '%s' at resource '%s' ", resourceRole, userCrn, resourceCrn));
        Log.whenJson(LOGGER, format(" Revoke resource role request:%n "), testDto.getRequest());
        LOGGER.info(format(" Revoking resource role '%s' from user '%s' at resource '%s'... ", resourceRole, userCrn, resourceCrn));
        Multimap<String, String> assignedResourceRoles = client.getDefaultClient().listAssignedResourceRoles(userCrn,
                regionAwareInternalCrnGeneratorFactory);
        if (assignedResourceRoles.get(resourceCrn).contains(resourceRole)) {
            client.getDefaultClient().unassignResourceRole(userCrn, resourceCrn, resourceRole,
                    regionAwareInternalCrnGeneratorFactory);
            // wait for UmsRightsCache to expire
            Thread.sleep(7000);
            LOGGER.info(format(" Resource role '%s' has been revoked from user '%s' at resource '%s' ", resourceRole, userCrn, resourceCrn));
            Log.when(LOGGER, format(" Resource role '%s' has been revoked from user '%s' at resource '%s' ", resourceRole, userCrn, resourceCrn));
        } else {
            LOGGER.info(format(" Resource role '%s' has already been revoked from user '%s' at resource '%s' ", resourceRole, userCrn, resourceCrn));
            Log.when(LOGGER, format(" Resource role '%s' has already been revoked from user '%s' at resource '%s' ", resourceRole, userCrn, resourceCrn));
        }
        return testDto;
    }
}
