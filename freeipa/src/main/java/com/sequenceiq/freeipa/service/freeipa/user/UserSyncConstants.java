package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

public final class UserSyncConstants {

    public static final String ADMINS_GROUP = "admins";

    public static final String CDP_USERSYNC_INTERNAL_GROUP = "cdp-usersync-internal";

    public static final String ENV_ASSIGNEES_GROUP_PREFIX = "_c_env_assignees";

    public static final Set<String> NON_POSIX_GROUPS = ImmutableSet.of(CDP_USERSYNC_INTERNAL_GROUP);

    public static final String ACCESS_ENVIRONMENT =
            AuthorizationResourceAction.ACCESS_ENVIRONMENT.getRight();

    public static final String ADMIN_FREEIPA =
            AuthorizationResourceAction.ADMIN_FREEIPA.getRight();

    public static final List<String> RIGHTS = ImmutableList.of(ACCESS_ENVIRONMENT, ADMIN_FREEIPA);

    public static final Predicate<String> ALLOWED_LARGE_GROUP_PREDICATE =
            groupName -> groupName.equals(CDP_USERSYNC_INTERNAL_GROUP) ||
                    groupName.startsWith(ENV_ASSIGNEES_GROUP_PREFIX);

    private UserSyncConstants() {
        // private access
    }
}
