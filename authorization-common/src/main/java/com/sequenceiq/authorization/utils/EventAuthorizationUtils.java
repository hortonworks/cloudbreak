package com.sequenceiq.authorization.utils;

import static com.sequenceiq.authorization.utils.GetAuthzActionTypeProvider.getActionsForResourceType;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

@Component
public class EventAuthorizationUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventAuthorizationUtils.class);

    private final CommonPermissionCheckingUtils permissionCheckingUtils;

    public EventAuthorizationUtils(CommonPermissionCheckingUtils permissionCheckingUtils) {
        this.permissionCheckingUtils = permissionCheckingUtils;
    }

    public void checkPermissionBasedOnResourceTypeAndCrn(Collection<EventAuthorizationDto> eventAuthorizationDtos) {
        if (isNotEmpty(eventAuthorizationDtos)) {
            LOGGER.info("Checking permissions for events: {}",
                    String.join(",", eventAuthorizationDtos.stream().map(EventAuthorizationDto::toString).collect(Collectors.toSet())));
            for (EventAuthorizationDto dto : eventAuthorizationDtos) {
                getActionsForResourceType(findResourceTypeByString(dto.getResourceType()))
                        .ifPresent(authzActionTypePair -> {
                            if (authzActionTypePair.hasAction()) {
                                checkPermissionForResource(authzActionTypePair.getResourceAction(), dto.getResourceCrn());
                            } else {
                                // CB-18797 - implement permission check mechanism for resources that has no designated authz action
                                LOGGER.info("The following {} ({}) has no designated {} to be checked for so at this point it is not possible to check right!",
                                        AuthorizationResourceType.class.getSimpleName(), authzActionTypePair.getResourceType(),
                                        AuthorizationResourceAction.class.getSimpleName());
                            }
                        });
            }
        } else {
            LOGGER.debug("No permisson check happened because the passed collection of {}s was null or empty.", EventAuthorizationDto.class.getSimpleName());
        }
    }

    private AuthorizationResourceType findResourceTypeByString(String resourceType) {
        try {
            return AuthorizationResourceType.valueOf(resourceType.toUpperCase());
        } catch (NullPointerException | IllegalArgumentException iae) {
            throw new IllegalStateException("Unable to find AuthZ action for resource: " + resourceType);
        }
    }

    private void checkPermissionForResource(AuthorizationResourceAction action, String resourceCrn) {
        LOGGER.debug("Checking permisson for action [{}] and resource CRN [{}]", action.name(), resourceCrn);
        permissionCheckingUtils.checkPermissionForUserOnResource(
                action,
                ThreadBasedUserCrnProvider.getUserCrn(),
                resourceCrn);
    }

}
