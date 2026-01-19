package com.sequenceiq.it.cloudbreak.assertion.ums;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.auth.altus.service.UmsResourceRole;
import com.sequenceiq.it.cloudbreak.action.ums.UmsClientUtils;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.UmsClient;

public class ResourceRoleTestAssertion {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceRoleTestAssertion.class);

    private ResourceRoleTestAssertion() {
    }

    public static Assertion<UmsTestDto, UmsClient> validateAssignedResourceRole(CloudbreakUser assignee, UmsResourceRole umsResourceRole,
            boolean expectedPresence) {
        return (testContext, umsTestDto, umsClient) -> {
            String resourceCrn = umsTestDto.getRequest().getResourceCrn();
            String userCrn = assignee.getCrn();
            String resourceRole = UmsClientUtils.getResourceRoleCrn(umsResourceRole, umsClient, resourceCrn, testContext);

            LOGGER.info(format(" Validate resource role '%s' has been successfully assigned to user '%s' at resource '%s'... ",
                    resourceRole, userCrn, resourceCrn));
            Multimap<String, String> assignedResourceRoles = umsClient.getDefaultClient(testContext).listAssignedResourceRoles(userCrn);
            boolean resourceRoleAssigned = assignedResourceRoles.get(resourceCrn).contains(resourceRole);
            if (expectedPresence) {
                if (resourceRoleAssigned) {
                    LOGGER.info(format(" Resource role '%s' has successfully been assigned to user '%s' at resource '%s' ", resourceRole, userCrn,
                            resourceCrn));
                    Log.then(LOGGER, format(" Resource role '%s' has successfully been assigned to user '%s' at resource '%s' ", resourceRole, userCrn,
                            resourceCrn));
                } else {
                    throw new TestFailException(format(" Resource role '%s' has not been assigned to user '%s' at resource '%s'! ", resourceRole,
                            userCrn, resourceCrn));
                }
            } else {
                if (!resourceRoleAssigned) {
                    LOGGER.info(format(" Resource role '%s' has successfully been revoked from user '%s' at resource '%s' ", resourceRole, userCrn,
                            resourceCrn));
                    Log.then(LOGGER, format(" Resource role '%s' has successfully been revoked from user '%s' at resource '%s' ", resourceRole,
                            userCrn, resourceCrn));
                } else {
                    throw new TestFailException(format(" Resource role '%s' has not been revoked from user '%s' at resource '%s'! ", resourceRole,
                            userCrn, resourceCrn));
                }
            }
            return umsTestDto;
        };
    }
}
