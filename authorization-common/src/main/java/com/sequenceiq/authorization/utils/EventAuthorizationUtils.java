package com.sequenceiq.authorization.utils;

import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

@Component
public class EventAuthorizationUtils {

    private CommonPermissionCheckingUtils permissionCheckingUtils;

    public EventAuthorizationUtils(CommonPermissionCheckingUtils permissionCheckingUtils) {
        this.permissionCheckingUtils = permissionCheckingUtils;
    }

    public void checkPermissionBasedOnResourceTypeAndCrn(Collection<EventAuthorizationDto> eventAuthorizationDtos) {
        throwIfNull(eventAuthorizationDtos,
                () -> new IllegalArgumentException("The collection of " + EventAuthorizationDto.class.getSimpleName() + "s should not be null!"));
        for (EventAuthorizationDto dto : eventAuthorizationDtos) {
            String resourceType = dto.getResourceType();
            Arrays.asList(AuthorizationResourceAction.values()).stream()
                    .filter(action -> isResourceTypeHasDescribeOrGetAction(action, resourceType))
                    .findFirst()
                    .ifPresentOrElse(
                            action -> checkPermissionForResource(action, dto.getResourceCrn()),
                            () -> checkHasLegacyActionOrThrow(resourceType)
                    );
        }
    }

    private boolean isResourceTypeHasDescribeOrGetAction(AuthorizationResourceAction action, String resourceType) {
        return (action.isDescribeAction() || action.isGetAction() || action.isAdminAction())
                && action.name().contains(resourceType.toUpperCase());
    }

    private void checkHasLegacyActionOrThrow(String resourceType) {
        if (!LegacyResourceTypes.getTypeValues().contains(resourceType) && !resourceType.toLowerCase().endsWith("dto")) {
            throwIllegalStateExceptionForResourceType(resourceType);
        }
    }

    private void checkPermissionForResource(AuthorizationResourceAction action, String resourceCrn) {
        permissionCheckingUtils.checkPermissionForUserOnResource(
                action,
                ThreadBasedUserCrnProvider.getUserCrn(),
                resourceCrn);
    }

    private void throwIllegalStateExceptionForResourceType(String resourceType) {
        throw new IllegalStateException("Unable to find AuthZ action for resource: " + resourceType);
    }

}
